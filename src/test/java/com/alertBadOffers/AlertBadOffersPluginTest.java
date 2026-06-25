package com.alertBadOffers;

import net.runelite.client.RuneLite;
import net.runelite.client.externalplugins.ExternalPluginManager;

public class AlertBadOffersPluginTest
{
	public static void main(String[] args) throws Exception
	{
		ExternalPluginManager.loadBuiltin(AlertBadOffersPlugin.class);
		RuneLite.main(args);
	}
}