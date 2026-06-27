package com.alertBadOffers;

import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

import javax.inject.Inject;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;


public class GeDeviationOverlay extends Overlay
{
    private final Client client;
    private final AlertBadOffersConfig config;
    private final WikiPriceService wikiPriceService;

    public static int lastAttemptedOfferHash = 0;

    @Inject
    public GeDeviationOverlay(Client client, AlertBadOffersConfig config, WikiPriceService wikiPriceService)
    {
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        this.client = client;
        this.config = config;
        this.wikiPriceService = wikiPriceService;
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

        Widget guidePriceWidget = client.getWidget(465, 28);
        if (guidePriceWidget == null || guidePriceWidget.isHidden()) return null;


        Widget parentWidget = client.getWidget(465, 26);
        if (parentWidget == null || parentWidget.isHidden()) return null;

        Widget[] structuralChildren = parentWidget.getChildren();
        if (structuralChildren == null || structuralChildren.length <= 41 || structuralChildren[41] == null || structuralChildren[23] == null) return null;

        int itemId = structuralChildren[23].getItemId();
        if (itemId <= 0) return null;

        // DETERMINE TARGET BASE PRICE
        long targetBasePrice = 0;
        if (config.useRealTimePrices())
        {
            targetBasePrice = wikiPriceService.getPrice(itemId);
        }

        // Fall back to normal GE price if config toggle is off OR if Wiki API hasn't resolved yet
        if (targetBasePrice <= 0)
        {
            targetBasePrice = parseLongSafely(guidePriceWidget.getText());
        }

        if (targetBasePrice <= 0) return null;

        long offerPrice = parseLongSafely(structuralChildren[41].getText());
        String quantityText = structuralChildren[34] != null ? structuralChildren[34].getText() : "1";
        long quantity = parseLongSafely(quantityText);
        if (quantity <= 0) quantity = 1;
        if (offerPrice <= 0) return null;

        long gpDifference = Math.abs(offerPrice - targetBasePrice);
        double deviation = ((double) gpDifference / targetBasePrice) * 100;
        long totalGpDifference = gpDifference * quantity;

        int currentOfferHash = java.util.Objects.hash(itemId, targetBasePrice, offerPrice, quantityText);
        if (currentOfferHash != lastAttemptedOfferHash)
        {
            lastAttemptedOfferHash = 0;
        }

        // EVALUATE NEW SLIDER BOUNDS
        boolean isPercentageViolation = deviation > config.customGeDeviationThreshold() && totalGpDifference >= config.minGeDeviationGpThreshold();
        boolean isMaxGpViolation = totalGpDifference >= config.maxGeDeviationGpThreshold();

        if (isPercentageViolation || isMaxGpViolation)
        {
            boolean isFirstClickBlocked = (currentOfferHash == lastAttemptedOfferHash);

            // 1. Prepare lines dynamically
            String line1 = "";
            String line2;

            if (config.useRealTimePrices())
            {
                line1 = String.format("1-Hour Average Mid price is: %,d GP", targetBasePrice);
            }

            if (isFirstClickBlocked)
            {
                line2 = isMaxGpViolation && !isPercentageViolation
                        ? String.format("⚠️ Confirm again to force submit (Deviates +%,d GP)", totalGpDifference)
                        : String.format("⚠️ Confirm again to force submit (Deviates %.1f%%)", deviation);
            }
            else
            {
                line2 = isMaxGpViolation && !isPercentageViolation
                        ? String.format("⚠️ Warning: Loss exceeds Max Limit (+%,d GP)!", totalGpDifference)
                        : String.format("⚠️ Warning: Price deviates by %.1f%%!", deviation);
            }

            Color accentColor = (isFirstClickBlocked) ? new Color(255, 152, 0) : new Color(239, 83, 80);

            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
            Font originalFont = graphics.getFont();
            Font warningFont = new Font("Dialog", Font.BOLD, 12);
            graphics.setFont(warningFont);

            FontMetrics metrics = graphics.getFontMetrics(warningFont);

            // 2. Calculate dimension metrics considering both lines
            boolean hasTwoLines = !line1.isEmpty();
            int textWidth = Math.max(metrics.stringWidth(line2), hasTwoLines ? metrics.stringWidth(line1) : 0);
            int lineHeight = metrics.getHeight();
            int textHeight = hasTwoLines ? (lineHeight * 2) + 2 : lineHeight; // Added 2px line spacing padding

            int paddingX = 14;
            int paddingY = 8;
            int boxWidth = textWidth + (paddingX * 2);
            int boxHeight = textHeight + (paddingY * 2);

            int x = parentWidget.getCanvasLocation().getX() + (parentWidget.getWidth() / 2) - (boxWidth / 2);
            // Move the box up slightly more if it has two lines to prevent overlap with buttons
            int y = parentWidget.getCanvasLocation().getY() + parentWidget.getHeight() - (hasTwoLines ? 90 : 74);

            graphics.setColor(new Color(25, 25, 25, 230));
            graphics.fillRect(x, y, boxWidth, boxHeight);

            graphics.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 160));
            graphics.drawRect(x, y, boxWidth, boxHeight);

            graphics.setColor(accentColor);
            int currentY = y + paddingY + metrics.getAscent();

            if (hasTwoLines)
            {
                int line1X = x + (boxWidth / 2) - (metrics.stringWidth(line1) / 2);
                graphics.setColor(Color.GRAY);
                graphics.drawString(line1, line1X, currentY);
                currentY += lineHeight + 2; // Advance cursor to next line space
            }

            int line2X = x + (boxWidth / 2) - (metrics.stringWidth(line2) / 2);
            graphics.setColor(accentColor);
            graphics.drawString(line2, line2X, currentY);

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