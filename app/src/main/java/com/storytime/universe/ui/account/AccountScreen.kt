package com.storytime.universe.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.AppConfig
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.theme.StColors
import com.storytime.universe.ui.util.openUrl

@Composable
fun AccountScreen(appState: AppState) {
    val context = LocalContext.current
    val profile = appState.activeProfile
    val user = appState.session?.user
    val sub = appState.subscription

    Column(
        Modifier.fillMaxSize().background(StColors.Background).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text("Account", color = StColors.Foreground, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 20.dp, top = 20.dp))

        // Profile header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                Modifier.size(64.dp).clip(CircleShape).background(StColors.profileColor(profile?.id ?: "x")),
                contentAlignment = Alignment.Center,
            ) {
                Text((profile?.name ?: "?").take(1).uppercase(), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(profile?.name ?: "Profile", color = StColors.Foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                user?.email?.let { Text(it, color = StColors.Muted, fontSize = 13.sp) }
                profile?.let { Text(it.ageLabel, color = StColors.AccentGold, fontSize = 12.sp, fontWeight = FontWeight.Medium) }
            }
        }

        // Subscription card
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(StColors.Card)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("Subscription", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(sub?.plan?.replaceFirstChar { it.uppercase() } ?: "No active plan", color = StColors.Foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                val status = sub?.status?.uppercase() ?: "—"
                val statusColor = when (status) {
                    "ACTIVE", "TRIALING" -> StColors.Accent
                    "PAST_DUE", "CANCELED", "CANCELLED", "EXPIRED" -> Color(0xFFE5484D)
                    else -> StColors.Muted
                }
                Text(status.replace("_", " "), color = statusColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clip(CircleShape).background(statusColor.copy(alpha = 0.15f)).padding(horizontal = 10.dp, vertical = 5.dp))
            }
            sub?.currentPeriodEnd?.let { Text("Renews / ends: $it", color = StColors.Muted, fontSize = 12.sp) }
            sub?.profileLimit?.let { Text("Profiles allowed: $it", color = StColors.Muted, fontSize = 12.sp) }

            if (appState.needsPaymentAttention) {
                ActionButton("Renew subscription", Icons.Filled.CreditCard, accent = true) { openUrl(context, AppConfig.RENEW_SUBSCRIPTION_URL) }
            } else {
                ActionButton("Manage subscription", Icons.Filled.CreditCard) { openUrl(context, AppConfig.ACCOUNT_URL) }
            }
            ActionButton("Change plan", Icons.AutoMirrored.Filled.OpenInNew) { openUrl(context, AppConfig.CHANGE_PLAN_URL) }
        }

        // Actions
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            ActionButton("Switch profile", Icons.Filled.SwapHoriz) { appState.switchProfile() }
            ActionButton("Privacy Policy", Icons.Filled.PrivacyTip) { openUrl(context, "${AppConfig.WEB_BASE_URL}/privacy") }
            ActionButton("Terms of Service", Icons.Filled.Description) { openUrl(context, "${AppConfig.WEB_BASE_URL}/terms") }
            ActionButton("Help & Support", Icons.Filled.HelpOutline) { openUrl(context, "${AppConfig.WEB_BASE_URL}/support") }
            ActionButton("Sign out", Icons.AutoMirrored.Filled.Logout, destructive = true) { appState.signOut() }
        }

        Text("Story Time Universe · v1.0", color = StColors.Muted.copy(alpha = 0.6f), fontSize = 12.sp, modifier = Modifier.padding(start = 20.dp, bottom = 32.dp))
    }
}

@Composable
private fun ActionButton(label: String, icon: ImageVector, accent: Boolean = false, destructive: Boolean = false, onClick: () -> Unit) {
    val tint = when {
        destructive -> Color(0xFFE5484D)
        accent -> StColors.Accent
        else -> StColors.Foreground
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (accent) StColors.Accent.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.06f))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Text(label, color = tint, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }
}
