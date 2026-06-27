# Alert Bad Offers

A lightweight RuneLite plugin that acts as a financial safety net in the Grand Exchange. It intercepts accidental over-payments or under-valuations before they hit the game server, protecting your hard-earned GP from catastrophic typos, misclicks, or outdated in-game guide prices.

## Features

* **Double-Click Confirmation Gate:** Intercepts the Grand Exchange "Confirm" button if your offer drastically deviates from your target price baseline, requiring a second intentional click to bypass and force-submit.
* **OSRS Wiki Real-Time Pricing:** Optional integration to fetch live, 1-hour price averages directly from the official OSRS Wiki Prices API. Perfect for volatile items and active flipping where the native Jagex guide price is inaccurate.
* **Dual-Layered GP Guardrails:** Combines percentage-based alerts with flat minimum and maximum absolute cash filters to maximize protection across massive item stacks.

## Configuration

You can fine-tune the strictness of the plugin within the standard RuneLite Configuration panel:

| Setting Name | Default Value | Description |
| :--- | :--- | :--- |
| **Price Deviation Warning (%)** | `10%` | The percentage threshold away from the target price that triggers a warning. |
| **Minimum GP Deviation** | `100,000 GP` | The absolute total transaction gold difference required to trigger a percentage warning (prevents annoying alerts on low-value items). |
| **Maximum GP Deviation** | `10,000,000 GP` | An absolute hard limit. Any offer exceeding this total transaction loss/gain will be flagged instantly, bypassing percentage rules. |
| **Use Real-Time 1-Hour Average** | `Disabled` | When enabled, the plugin calculates deviations against the live 1-hour Wiki average instead of the standard GE guide price. |

## How It Works

1. **Live Data Fetching & Caching:** When real-time pricing is enabled, the plugin asynchronously pulls the 1-hour volume averages from the Wiki API. Responses are safely cached for **5 minutes** to avoid flooding the API with requests. If the API is unreachable, it seamlessly falls back to the standard GE price.
2. **First Click (Intercepted):** If your offer breaches either your percentage or total maximum GP thresholds, the first click on "Confirm" is safely consumed and blocked. The HUD switches to an **Orange Action Notice**.
3. **Second Click (Passed Through):** Clicking "Confirm" again without changing the offer parameters bypasses the gate and submits the trade cleanly to the server.
4. **Auto-Reset:** Changing the price, quantity, or switching items automatically resets the security gate state to prevent accidental double-clicks from bleeding over into your next trade.
