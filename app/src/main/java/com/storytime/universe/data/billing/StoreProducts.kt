package com.storytime.universe.data.billing

/**
 * Google Play product IDs — create matching products in Play Console with these exact IDs
 * (mirrors iOS StoreKit IDs).
 *
 * Plan codes match production `VIEWER_PLAN_CONFIG` (BASE_1 / STANDARD_3 / FAMILY_5 / PPV_FILM).
 */
object StoreProducts {

    const val BASE_MONTHLY = "com.storytime.universe.sub.base.monthly"
    const val STANDARD_MONTHLY = "com.storytime.universe.sub.standard.monthly"
    const val FAMILY_MONTHLY = "com.storytime.universe.sub.family.monthly"

    const val BASE_YEARLY = "com.storytime.universe.sub.base.yearly"
    const val STANDARD_YEARLY = "com.storytime.universe.sub.standard.yearly"
    const val FAMILY_YEARLY = "com.storytime.universe.sub.family.yearly"

    /** One-time title unlock — R49.99, 7 days of access (consumable / one-time product). */
    const val PPV_UNLOCK = "com.storytime.universe.ppv.unlock"

    const val PPV_PRICE_ZAR = 49.99
    const val PPV_ACCESS_DAYS = 7

    val allSubscriptionIds: List<String> = listOf(
        BASE_MONTHLY, STANDARD_MONTHLY, FAMILY_MONTHLY,
        BASE_YEARLY, STANDARD_YEARLY, FAMILY_YEARLY,
    )

    data class PlanOffer(
        val productId: String,
        val planCode: String,
        val displayName: String,
        val listPriceZar: Double,
        val isYearly: Boolean,
        val profileLimit: Int,
        val deviceLabel: String,
        val features: List<String>,
        val highlight: Boolean = false,
    )

    val monthlyOffers: List<PlanOffer> = listOf(
        PlanOffer(
            productId = BASE_MONTHLY,
            planCode = "BASE_1",
            displayName = "Basic",
            listPriceZar = 29.99,
            isYearly = false,
            profileLimit = 1,
            deviceLabel = "1",
            features = listOf(
                "Full Story Time catalogue",
                "1 profile · 1 device",
                "HD streaming & downloads",
                "Cancel anytime in Play Store",
            ),
        ),
        PlanOffer(
            productId = STANDARD_MONTHLY,
            planCode = "STANDARD_3",
            displayName = "Standard",
            listPriceZar = 89.99,
            isYearly = false,
            profileLimit = 3,
            deviceLabel = "3",
            features = listOf(
                "Full Story Time catalogue",
                "3 profiles · 3 devices",
                "Share with family",
                "HD streaming & downloads",
                "Cancel anytime in Play Store",
            ),
            highlight = true,
        ),
        PlanOffer(
            productId = FAMILY_MONTHLY,
            planCode = "FAMILY_5",
            displayName = "Premium",
            listPriceZar = 119.99,
            isYearly = false,
            profileLimit = 5,
            deviceLabel = "5+",
            features = listOf(
                "Full Story Time catalogue",
                "5 profiles · 5+ devices",
                "Best for households",
                "HD streaming & downloads",
                "Cancel anytime in Play Store",
            ),
        ),
    )

    val yearlyOffers: List<PlanOffer> = listOf(
        PlanOffer(
            productId = BASE_YEARLY,
            planCode = "BASE_1",
            displayName = "Basic",
            listPriceZar = 299.99,
            isYearly = true,
            profileLimit = 1,
            deviceLabel = "1",
            features = monthlyOffers[0].features,
        ),
        PlanOffer(
            productId = STANDARD_YEARLY,
            planCode = "STANDARD_3",
            displayName = "Standard",
            listPriceZar = 899.99,
            isYearly = true,
            profileLimit = 3,
            deviceLabel = "3",
            features = monthlyOffers[1].features,
            highlight = true,
        ),
        PlanOffer(
            productId = FAMILY_YEARLY,
            planCode = "FAMILY_5",
            displayName = "Premium",
            listPriceZar = 1_199.99,
            isYearly = true,
            profileLimit = 5,
            deviceLabel = "5+",
            features = monthlyOffers[2].features,
        ),
    )

    fun offers(yearly: Boolean): List<PlanOffer> = if (yearly) yearlyOffers else monthlyOffers

    fun offerFor(productId: String): PlanOffer? =
        (monthlyOffers + yearlyOffers).firstOrNull { it.productId == productId }

    fun planCode(productId: String): String = when (productId) {
        PPV_UNLOCK -> "PPV_FILM"
        else -> offerFor(productId)?.planCode ?: "BASE_1"
    }

    fun displayName(productId: String): String = when (productId) {
        PPV_UNLOCK -> "Title Unlock"
        else -> offerFor(productId)?.displayName ?: "Story Time"
    }

    fun displayNameForPlanCode(plan: String?): String = when (plan?.uppercase()) {
        "BASE_1", "BASE", "BASIC" -> "Basic"
        "STANDARD_3", "STANDARD" -> "Standard"
        "FAMILY_5", "FAMILY", "PREMIUM" -> "Premium"
        "PPV_FILM", "PPV", "PAY_PER_VIEW" -> "Pay Per View"
        else -> plan?.replace("_", " ")?.replaceFirstChar { it.uppercase() } ?: "No active plan"
    }

    fun formatZar(amount: Double): String {
        val whole = amount.toInt()
        val cents = ((amount - whole) * 100).toInt().coerceIn(0, 99)
        return if (cents == 0) "R$whole" else "R$whole.${cents.toString().padStart(2, '0')}"
    }
}

/** Why the paywall is shown — controls copy (iOS `PaywallContext`). */
sealed class PaywallContext {
    data object Subscribe : PaywallContext()
    data object Reactivate : PaywallContext()
    data object ChangePlan : PaywallContext()
    data class Ppv(val contentId: String, val title: String?) : PaywallContext()

    val isPpv: Boolean get() = this is Ppv
}
