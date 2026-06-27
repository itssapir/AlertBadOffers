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

	@ConfigItem(
			keyName = "maxGeDeviationGpThreshold",
			name = "Maximum GP Deviation",
			description = "The absolute maximum GP difference allowed. Any offer exceeding this flat cash difference will be flagged instantly, regardless of percentages.",
			position = 3
	)
	default int maxGeDeviationGpThreshold()
	{
		return 10000000; // Default to 10M GP limit
	}

	@ConfigItem(
			keyName = "useRealTimePrices",
			name = "Use Real-Time 1-Hour Average",
			description = "Use the live OSRS Wiki 1-hour average prices for calculations instead of the in-game GE guide price.",
			position = 4
	)
	default boolean useRealTimePrices()
	{
		return false;
	}
}
