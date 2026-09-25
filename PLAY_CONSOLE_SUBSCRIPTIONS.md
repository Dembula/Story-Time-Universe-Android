# Google Play product setup — Story Time Universe

Play Console needs an **APK/AAB uploaded** before you can create products. After you upload a build from Android Studio, create these products.

## Subscription group — Story Time Universe

### Monthly (required)

| Product ID | Name | Price (ZAR) | Server plan |
|------------|------|-------------|-------------|
| `com.storytime.universe.sub.base.monthly` | Basic | **R 29.99** / month | `BASE_1` |
| `com.storytime.universe.sub.standard.monthly` | Standard | **R 89.99** / month | `STANDARD_3` |
| `com.storytime.universe.sub.family.monthly` | Premium | **R 119.99** / month | `FAMILY_5` |

### Yearly (optional)

| Product ID | Price (ZAR) |
|------------|-------------|
| `com.storytime.universe.sub.base.yearly` | R 299.99 / year |
| `com.storytime.universe.sub.standard.yearly` | R 899.99 / year |
| `com.storytime.universe.sub.family.yearly` | R 1 199.99 / year |

## Pay Per View (one-time / consumable)

| Product ID | Name | Price | Access |
|------------|------|-------|--------|
| `com.storytime.universe.ppv.unlock` | Title Unlock | **R 49.99** | **7 days** per title |

Create this as a **one-time product** (managed product / consumable), not a subscription.

## How PPV works in the app

1. At signup / package pick: choose **Subscription** or **Pay Per View**.
2. PPV accounts pay nothing monthly — browse freely.
3. On a title, the button says **Pay** → unlock for R49.99 → then play for **7 days**.
4. After payment, playback starts automatically when Google/web unlock succeeds.

## Until Play products are live

- Subscription checkout falls back to `https://story-time.online/onboarding/package`
- PPV unlock falls back to PayFast via `POST /api/viewer/ppv` → `checkoutUrl`

## Server note

Google Play purchase verification endpoints (`/api/viewer/google/*`) should mark `ViewerContentAccess` COMPLETED with **+7 days** expiry for PPV.
