package com.storytime.universe.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.AppConfig
import com.storytime.universe.data.billing.PaywallContext
import com.storytime.universe.data.billing.StoreProducts
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.theme.StColors
import com.storytime.universe.ui.util.openUrl

@Composable
fun SubscriptionInfoScreen(appState: AppState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val subscription = appState.subscription

    LaunchedEffect(Unit) {
        appState.refreshSubscription()
    }

    val planTitle = StoreProducts.displayNameForPlanCode(subscription?.plan).let {
        when {
            it.isNotBlank() && it != "Subscription" -> it
            appState.isPayPerViewAccount -> "Pay Per View"
            else -> "No active plan"
        }
    }

    val statusLabel = when (subscription?.status?.uppercase()) {
        "ACTIVE", "TRIALING", "PAID" -> "Active"
        "PAST_DUE" -> "Past due"
        "CANCELED", "CANCELLED" -> "Canceled"
        "EXPIRED" -> "Expired"
        null, "" -> "Inactive"
        else -> subscription.status!!.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
    }
    val statusColor = when (statusLabel) {
        "Active" -> Color(0xFF3DDC84)
        "Past due" -> Color(0xFFFF9800)
        "Canceled", "Expired" -> Color(0xFFE5484D)
        else -> StColors.Muted
    }

    val renewalLine = subscription?.currentPeriodEnd?.trim()?.takeIf { it.isNotEmpty() }?.let { end ->
        if (subscription.cancelAtPeriodEnd == true) "Access ends ${friendlyDate(end)}"
        else "Renews ${friendlyDate(end)}"
    }

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        SheetHeader(title = "Subscription", onDismiss = onDismiss)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(StColors.Card)
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(planTitle, color = StColors.Foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text(
                        statusLabel,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(statusColor.copy(alpha = 0.15f))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    )
                }

                when {
                    appState.isPayPerViewAccount -> SubRow("Model", "Unlock titles individually")
                    subscription?.plan?.uppercase() in setOf("BASE_1", "BASE", "BASIC") ->
                        SubRow("Price", "${StoreProducts.formatZar(29.99)} / month")
                    subscription?.plan?.uppercase() in setOf("STANDARD_3", "STANDARD") ->
                        SubRow("Price", "${StoreProducts.formatZar(89.99)} / month")
                    subscription?.plan?.uppercase() in setOf("FAMILY_5", "FAMILY", "PREMIUM") ->
                        SubRow("Price", "${StoreProducts.formatZar(119.99)} / month")
                }

                renewalLine?.let { SubRow("Billing", it) }
                subscription?.profileLimit?.let { SubRow("Profiles", "Up to $it") }
                subscription?.deviceCount?.let { SubRow("Devices", "$it") }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryAction("Change Plan") {
                    onDismiss()
                    appState.presentPaywall(PaywallContext.ChangePlan)
                }
                SecondaryAction("Manage in Play Store / web") {
                    openUrl(context, AppConfig.ACCOUNT_URL)
                }
                if (appState.needsPaymentAttention) {
                    SecondaryAction("Reactivate Subscription", gold = true) {
                        onDismiss()
                        appState.presentPaywall(PaywallContext.Reactivate)
                    }
                }
            }

            Text(
                "Subscriptions can be billed through Google Play or managed on story-time.online. Change plan, renew, or cancel from Account settings.",
                color = StColors.Muted,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun SubRow(title: String, value: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Text(title, color = StColors.Muted, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Text(value, color = StColors.Foreground, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
internal fun PrimaryAction(label: String, onClick: () -> Unit) {
    Text(
        label,
        color = Color.Black,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StColors.Accent)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

@Composable
internal fun SecondaryAction(label: String, gold: Boolean = false, onClick: () -> Unit) {
    val color = if (gold) StColors.AccentGold else StColors.Accent
    Text(
        label,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
    )
}

private fun friendlyDate(raw: String): String {
    val prefix = raw.substringBefore('T').takeIf { it.isNotEmpty() }
    return prefix ?: raw
}
