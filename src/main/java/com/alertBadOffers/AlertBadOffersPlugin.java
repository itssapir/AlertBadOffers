package com.alertBadOffers;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Client;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.widgets.Widget;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

@Slf4j
@PluginDescriptor(
	name = "Alert Bad Offers"
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

	@Inject
	private WikiPriceService wikiPriceService;

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
		wikiPriceService.clearCache();
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
		if (option != null && option.equalsIgnoreCase("Confirm"))
		{
			Widget guidePriceWidget = client.getWidget(465, 28);
			Widget parentWidget = client.getWidget(465, 26);

			if (guidePriceWidget == null || parentWidget == null) return;

			Widget[] structuralChildren = parentWidget.getChildren();
			if (structuralChildren == null || structuralChildren.length <= 41 || structuralChildren[41] == null || structuralChildren[23] == null) return;

			int itemId = structuralChildren[23].getItemId();
			if (itemId <= 0) return;

			// DETERMINE TARGET BASE PRICE
			long targetBasePrice = 0;
			if (config.useRealTimePrices())
			{
				targetBasePrice = wikiPriceService.getPrice(itemId);
			}
			if (targetBasePrice <= 0)
			{
				targetBasePrice = GeDeviationOverlay.parseLongSafely(guidePriceWidget.getText());
			}

			long offerPrice = GeDeviationOverlay.parseLongSafely(structuralChildren[41].getText());
			String quantityText = structuralChildren[34] != null ? structuralChildren[34].getText() : "1";
			long quantity = GeDeviationOverlay.parseLongSafely(quantityText);
			if (quantity <= 0) quantity = 1;

			if (targetBasePrice <= 0 || offerPrice <= 0) return;

			long gpDifference = Math.abs(offerPrice - targetBasePrice);
			double deviation = ((double) gpDifference / targetBasePrice) * 100;
			long totalGpDifference = gpDifference * quantity;

			// MULTI-GUARD FILTER EVALUATION
			boolean isPercentageViolation = deviation > config.customGeDeviationThreshold() && totalGpDifference >= config.minGeDeviationGpThreshold();
			boolean isMaxGpViolation = totalGpDifference >= config.maxGeDeviationGpThreshold();

			if (isPercentageViolation || isMaxGpViolation) {
				int currentOfferHash = java.util.Objects.hash(itemId, targetBasePrice, offerPrice, quantityText);

				if (currentOfferHash != GeDeviationOverlay.lastAttemptedOfferHash)
				{
					event.consume();
					GeDeviationOverlay.lastAttemptedOfferHash = currentOfferHash;
				}
			}
		}
	}

}
