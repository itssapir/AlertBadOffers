package com.alertBadOffers;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("AlertBadOffers")
public interface AlertBadOffersConfig extends Config
{
	@ConfigItem(
			keyName = "customGeDeviationThreshold",
			name = "Price Deviation Warning (%)",
			description = "Triggers a custom warning panel if your offer price differs from the guide price by more than this percentage.",
			position = 1
	)
	default int customGeDeviationThreshold()
	{
		return 10; // Default to a 10% tolerance limit
	}
	@ConfigItem(
			keyName = "minGeDeviationGpThreshold",
			name = "Minimum GP Deviation",
			description = "The minimum absolute GP difference required to trigger the warning banner.",
			position = 2
	)
	default int minGeDeviationGpThreshold()
	{
		return 100000; // Default to 100,000 gp (100k) minimum difference
	}
}
