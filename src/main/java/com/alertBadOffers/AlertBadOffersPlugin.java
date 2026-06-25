package com.alertBadOffers;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import java.util.Objects;

@Slf4j
@PluginDescriptor(
	name = "AlertBadOffers"
)
public class AlertBadOffersPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private AlertBadOffersConfig config;

	@Inject
	private OverlayManager overlayManager;
	@Inject
	private GeDeviationOverlay geDeviationOverlay;

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(geDeviationOverlay);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(geDeviationOverlay);
		GeDeviationOverlay.lastAttemptedOfferHash = 0;
	}

	@Provides
	AlertBadOffersConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(AlertBadOffersConfig.class);
	}

	@Subscribe
	public void onMenuOptionClicked(MenuOptionClicked event)
	{
		String option = event.getMenuOption();
		if (option != null && option.equalsIgnoreCase("Confirm")) //
		{
			Widget itemWidget = client.getWidget(465, 21);
			int itemId = itemWidget != null ? itemWidget.getItemId() : 0;

			Widget guidePriceWidget = client.getWidget(465, 28); //
			Widget parentWidget = client.getWidget(465, 26); //

			if (guidePriceWidget == null || parentWidget == null) return;

			Widget[] structuralChildren = parentWidget.getChildren();
			if (structuralChildren == null || structuralChildren.length <= 41 || structuralChildren[41] == null) return;

			long guidePrice = GeDeviationOverlay.parseLongSafely(guidePriceWidget.getText()); //
			long offerPrice = GeDeviationOverlay.parseLongSafely(structuralChildren[41].getText()); //
			String quantityText = structuralChildren[34] != null ? structuralChildren[34].getText() : "1"; //
			long quantity = GeDeviationOverlay.parseLongSafely(quantityText);
			if (quantity <= 0) quantity = 1; // Fallback guard
			if (guidePrice <= 0 || offerPrice <= 0) return;

			// Calculate both the raw gold coin difference and percentage deviation
			long gpDifference = Math.abs(offerPrice - guidePrice);
			double deviation = ((double) gpDifference / guidePrice) * 100;

			// DOUBLE GUARD CHECK: Must violate both the percentage AND the raw GP threshold limits
			if (deviation > config.customGeDeviationThreshold() && gpDifference*quantity >= config.minGeDeviationGpThreshold())
			{
				int currentOfferHash = java.util.Objects.hash(itemId, guidePrice, offerPrice, quantityText);

				if (currentOfferHash != GeDeviationOverlay.lastAttemptedOfferHash)
				{
					event.consume();
					GeDeviationOverlay.lastAttemptedOfferHash = currentOfferHash;
				}
			}
		}
	}

}
