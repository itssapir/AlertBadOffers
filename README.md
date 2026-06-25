# Alert Bad Offers

A lightweight RuneLite plugin that acts as a financial safety net in the Grand Exchange. It intercepts accidental over-payments or under-valuations before they hit the game server, protecting your hard-earned GP from catastrophic typos or misclicks.

## Features

* **Double-Click Confirmation Gate:** Intercepts the Grand Exchange "Confirm" button if your offer drastically deviates from the Jagex guide price, requiring a second intentional click to bypass and force-submit.
* **Dynamic Warning Banner:** Renders a clean, high-visibility visual warning directly inside the Grand Exchange interface right above your action zone.
* **Proportional Loss Awareness:** Takes transaction quantities into account. It intelligently ignores minor 5 GP adjustments on single items but wakes up instantly if a tiny price mistake scales into a massive loss across a stack of 10,000 items.

## Configuration

You can fine-tune the strictness of the plugin within the standard RuneLite Configuration panel:

| Setting Name | Default Value | Description |
| :--- | :--- | :--- |
| **Price Deviation Warning (%)** | `10%` | The percentage threshold away from the guide price that triggers a warning. |
| **Minimum GP Deviation** | `100,000 GP` | The absolute total transaction gold difference required to trigger the alert. (Protects you from annoying alerts on low-value items). |

## How It Works

1. **First Click (Intercepted):** If your offer breaches both the percentage and total GP thresholds, the first click on "Confirm" is consumed and blocked. The visual banner changes from a **Red Warning** to an **Orange Action Notice**.
2. **Second Click (Passed Through):** Clicking "Confirm" again while the item layout remains unchanged passes the transaction cleanly through to the game server.
3. **Auto-Reset:** Changing the price, quantity, or switching items automatically resets the security gate to prevent accidental double-clicks from bleeding over into your next trade.
