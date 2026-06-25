package com.alertBadOffers;

import com.alertBadOffers.AlertBadOffersConfig;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.*;

public class GeDeviationOverlay extends Overlay
{
    private final Client client;
    private final AlertBadOffersConfig config;

    // Static state tracker decouples the overlay from FlippingPlugin entirely
    public static int lastAttemptedOfferHash = 0;

    @Inject
    public GeDeviationOverlay(Client client, AlertBadOffersConfig config)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.client = client;
        this.config = config;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Widget headerWidget = client.getWidget(465, 2);
        if (headerWidget == null || headerWidget.isHidden()) return null;


        Widget[] headerChildren = headerWidget.getChildren();
        if (headerChildren == null || headerChildren.length < 2 || headerChildren[1] == null) return null;

        if (!headerChildren[1].getText().contains("Set up offer")) {
            lastAttemptedOfferHash = 0; // Possible cleanup after last offer was made
            return null;
        }

        Widget itemWidget = client.getWidget(465, 21);
        int itemId = itemWidget != null ? itemWidget.getItemId() : 0;

        Widget guidePriceWidget = client.getWidget(465, 28);
        if (guidePriceWidget == null || guidePriceWidget.isHidden()) return null;
        
        long guidePrice = parseLongSafely(guidePriceWidget.getText());
        Widget parentWidget = client.getWidget(465, 26);
        if (parentWidget == null || parentWidget.isHidden() || guidePrice <= 0) return null;

        Widget[] structuralChildren = parentWidget.getChildren();
        if (structuralChildren == null || structuralChildren.length <= 41 || structuralChildren[41] == null) return null;

        long offerPrice = parseLongSafely(structuralChildren[41].getText());
        String quantityText = structuralChildren[34] != null ? structuralChildren[34].getText() : "1";
        long quantity = parseLongSafely(quantityText);
        if (quantity <= 0) quantity = 1;
        if (offerPrice <= 0) return null;

        long gpDifference = Math.abs(offerPrice - guidePrice);
        double deviation = ((double) gpDifference / guidePrice) * 100;

        int currentOfferHash = java.util.Objects.hash(itemId, guidePrice, offerPrice, quantityText);
        if (currentOfferHash != lastAttemptedOfferHash)
        {
            lastAttemptedOfferHash = 0;
        }

        if (deviation > config.customGeDeviationThreshold() && gpDifference*quantity >= config.minGeDeviationGpThreshold())
        {
            boolean isFirstClickBlocked = (currentOfferHash == lastAttemptedOfferHash);

            // Configure sleek text and color themes
            String warningText = (isFirstClickBlocked)
                ? String.format("⚠️ Confirm again to force submit (Deviates %.1f%%)", deviation)
                : String.format("⚠️ Warning: Price deviates by %.1f%%!", deviation);
            
            Color accentColor = (isFirstClickBlocked) ? new Color(255, 152, 0) : new Color(239, 83, 80);

            // Set up clean typography rendering
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            Font originalFont = graphics.getFont();
            Font warningFont = new Font("Dialog", Font.BOLD, 12);
            graphics.setFont(warningFont);

            FontMetrics metrics = graphics.getFontMetrics(warningFont);
            int textWidth = metrics.stringWidth(warningText);
            int textHeight = metrics.getHeight();

            // Structure a beautiful, clean UI banner panel directly above the confirmation interface zone
            int paddingX = 14;
            int paddingY = 8;
            int boxWidth = textWidth + (paddingX * 2);
            int boxHeight = textHeight + (paddingY * 2);
            
            int x = parentWidget.getCanvasLocation().getX() + (parentWidget.getWidth() / 2) - (boxWidth / 2);
            int y = parentWidget.getCanvasLocation().getY() + parentWidget.getHeight() - 74;

            // Draw sleek dark background drop-shadow container plate
            graphics.setColor(new Color(25, 25, 25, 230));
            graphics.fillRect(x, y, boxWidth, boxHeight);

            // Draw thin framing boundary accent line
            graphics.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 160));
            graphics.drawRect(x, y, boxWidth, boxHeight);

            // Print the dynamic string notification context cleanly inside the box boundaries
            graphics.setColor(accentColor);
            graphics.drawString(warningText, x + paddingX, y + paddingY + metrics.getAscent());

            // Reset graphics configuration states back safely
            graphics.setFont(originalFont);
        }

        return null;
    }

    public static long parseLongSafely(String text)
    {
        if (text == null) return 0;
        try
        {
            String cleanText = text.replaceAll("[^0-9]", "");
            return cleanText.isEmpty() ? 0 : Long.parseLong(cleanText);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }
}