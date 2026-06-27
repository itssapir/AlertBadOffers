package com.alertBadOffers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

@Slf4j
@Singleton
public class WikiPriceService
{
    private static final String API_URL = "https://prices.runescape.wiki/api/v1/osrs/1h";
    private static final int CACHE_DURATION_MINUTES = 5;

    private final OkHttpClient httpClient;
    private final Gson gson;

    private final Map<Integer, Long> priceCache = new HashMap<>();
    private Instant cacheExpiry = Instant.MIN;
    private boolean isFetching = false;

    @Inject
    public WikiPriceService(OkHttpClient httpClient, Gson gson)
    {
        this.httpClient = httpClient;
        this.gson = gson;
    }

    /**
     * Gets the 1-hour average price for an item.
     * Returns 0 if data isn't loaded yet or if the item isn't in the API response.
     */
    public long getPrice(int itemId)
    {
        if (Instant.now().isAfter(cacheExpiry))
        {
            fetchPrices();
        }
        return priceCache.getOrDefault(itemId, 0L);
    }

    private synchronized void fetchPrices()
    {
        if (isFetching) return;
        isFetching = true;

        priceCache.clear();

        HttpUrl url = HttpUrl.parse(API_URL);
        if (url == null)
        {
            log.error("Failed to parse Wiki API URL: {}", API_URL);
            isFetching = false;
            return;
        }

        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "AlertBadOffers-RuneLitePlugin")
                .build();

        httpClient.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(@Nonnull Call call, @Nonnull IOException e)
            {
                log.error("Failed to fetch OSRS Wiki real-time 1h prices", e);
                isFetching = false;
                cacheExpiry = Instant.now().plusSeconds(30);
            }

            @Override
            public void onResponse(@Nonnull Call call, @Nonnull Response response)
            {
                try (Response r = response)
                {
                    if (!r.isSuccessful() || r.body() == null)
                    {
                        isFetching = false;
                        cacheExpiry = Instant.now().plusSeconds(30);
                        return;
                    }

                    JsonObject root = gson.fromJson(new InputStreamReader(r.body().byteStream()), JsonObject.class);
                    if (root != null && root.has("data"))
                    {
                        JsonObject data = root.getAsJsonObject("data");
                        for (Map.Entry<String, JsonElement> entry : data.entrySet())
                        {
                            try
                            {
                                int id = Integer.parseInt(entry.getKey());
                                JsonObject fields = entry.getValue().getAsJsonObject();

                                // Call the clean, extracted helper method
                                long midPrice = calculateMidPrice(fields);

                                if (midPrice > 0)
                                {
                                    priceCache.put(id, midPrice);
                                }
                            }
                            catch (Exception ignored) {}
                        }
                    }
                    cacheExpiry = Instant.now().plus(CACHE_DURATION_MINUTES, java.time.temporal.ChronoUnit.MINUTES);
                }
                catch (Exception e)
                {
                    log.error("Error parsing wiki prices JSON", e);
                }
                finally
                {
                    isFetching = false;
                }
            }

            private long calculateMidPrice(JsonObject fields)
            {
                long avgHigh = fields.has("avgHighPrice") && !fields.get("avgHighPrice").isJsonNull() ? fields.get("avgHighPrice").getAsLong() : 0;
                long avgLow = fields.has("avgLowPrice") && !fields.get("avgLowPrice").isJsonNull() ? fields.get("avgLowPrice").getAsLong() : 0;

                if (avgHigh > 0 && avgLow > 0)
                {
                    return (avgHigh + avgLow) / 2;
                }
                else if (avgHigh > 0)
                {
                    return avgHigh;
                }
                else if (avgLow > 0)
                {
                    return avgLow;
                }

                return 0;
            }
        });
    }

    public void clearCache()
    {
        priceCache.clear();
        cacheExpiry = Instant.MIN;
    }
}
