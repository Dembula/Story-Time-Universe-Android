# Google Play product setup — Story Time Universe

Play Console needs an **AAB uploaded** (versionCode ≥ 2) built with **Play Billing Library 8.0.0+** before products can be sold. Product IDs must be **≤ 40 characters**.

Subscription group name: **Story Time Universe**

---

## Subscriptions — product IDs & names

Create **6 auto-renewing subscriptions** (one product per plan × period).

| Product ID | Product name | Len | Price (ZAR) | Server plan |
|------------|--------------|-----|-------------|-------------|
| `stu.sub.base.monthly` | **Basic Monthly** | 20 | R 29.99 / month | `BASE_1` |
| `stu.sub.standard.monthly` | **Standard Monthly** | 24 | R 89.99 / month | `STANDARD_3` |
| `stu.sub.premium.monthly` | **Premium Monthly** | 23 | R 119.99 / month | `FAMILY_5` |
| `stu.sub.base.yearly` | **Basic Yearly** | 19 | R 299.99 / year | `BASE_1` |
| `stu.sub.standard.yearly` | **Standard Yearly** | 23 | R 899.99 / year | `STANDARD_3` |
| `stu.sub.premium.yearly` | **Premium Yearly** | 22 | R 1 199.99 / year | `FAMILY_5` |

### Base plans (required on each subscription)

Each product above needs **exactly one** base plan:

| Product ID | Base plan ID | Base plan name | Renewal |
|------------|--------------|----------------|---------|
| `stu.sub.base.monthly` | `monthly` | **Monthly** | Every 1 month |
| `stu.sub.standard.monthly` | `monthly` | **Monthly** | Every 1 month |
| `stu.sub.premium.monthly` | `monthly` | **Premium Monthly** → use name **Monthly** | Every 1 month |
| `stu.sub.base.yearly` | `yearly` | **Yearly** | Every 1 year |
| `stu.sub.standard.yearly` | `yearly` | **Yearly** | Every 1 year |
| `stu.sub.premium.yearly` | `yearly` | **Yearly** | Every 1 year |

Use base plan IDs exactly: **`monthly`** and **`yearly`** (the app selects offers by `basePlanId`).

Activate each base plan for the countries you sell in (at least **South Africa / ZAR**).

---

## Pay Per View (one-time / consumable)

| Product ID | Product name | Len | Price | Access |
|------------|--------------|-----|-------|--------|
| `stu.ppv.unlock` | **Title Unlock** | 14 | **R 49.99** | **7 days** per title |

Create as a **one-time product** (managed / consumable), **not** a subscription.

---

## How PPV works in the app

1. At signup / package pick: choose **Subscription** or **Pay Per View**.
2. PPV accounts pay nothing monthly — browse freely.
3. On a title, the button says **Pay** → unlock for R49.99 → then play for **7 days**.
4. After payment, playback starts automatically when Google/web unlock succeeds.

## Until Play products are live

- Subscription checkout falls back to `https://story-time.online/onboarding/package`
- PPV unlock falls back to PayFast via `POST /api/viewer/ppv` → `checkoutUrl`

## Server note

Google Play purchase verification endpoints (`/api/viewer/google/*`) should:

- Map `stu.sub.*` product IDs → `BASE_1` / `STANDARD_3` / `FAMILY_5` (app also sends `planCode`)
- Mark `ViewerContentAccess` COMPLETED with **+7 days** expiry for PPV (`stu.ppv.unlock`)

## Billing library

App depends on `com.android.billingclient:billing-ktx:8.0.0` (Play Console requires ≥ 8.0.0).
Rebuild and upload a **new AAB** after changing the Billing library version.
