package com.storytime.universe.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.storytime.universe.data.billing.PaywallContext
import com.storytime.universe.data.billing.StoreProducts
import com.storytime.universe.data.parental.ParentalControls
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.theme.StColors

private enum class AccountSheet {
    None,
    AccountInfo,
    Subscription,
    PlaybackHelp,
    Parental,
    AgeAssurance,
    DeleteAccount,
}

@Composable
fun AccountScreen(appState: AppState) {
    val context = LocalContext.current
    val parental = remember { ParentalControls.get(context) }
    var sheet by remember { mutableStateOf(AccountSheet.None) }

    val profile = appState.activeProfile
    val planLabel = StoreProducts.displayNameForPlanCode(appState.subscription?.plan)
    val status = appState.subscription?.status ?: "—"

    Column(
        Modifier
            .fillMaxSize()
            .background(StColors.Background)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text("Account", color = StColors.Foreground, fontSize = 32.sp, fontWeight = FontWeight.Bold)

        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f))
                .clickable { sheet = AccountSheet.AccountInfo }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                StColors.profileColor(profile?.id ?: "a"),
                                StColors.Accent.copy(alpha = 0.7f),
                            ),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    (profile?.name ?: "?").take(1).uppercase(),
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    profile?.name ?: appState.session?.user?.name ?: "Account",
                    color = StColors.Foreground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text("Account info, subscription and settings", color = StColors.Muted, fontSize = 12.sp)
            }
            Text("›", color = StColors.Muted, fontSize = 18.sp)
        }

        SettingsGroup {
            SettingsRow(
                title = "Account info",
                subtitle = appState.session?.user?.email ?: "Name, email, phone, address and plan",
                icon = Icons.Filled.Person,
            ) { sheet = AccountSheet.AccountInfo }
        }

        SettingsGroup {
            SettingsRow(
                title = "Subscription",
                subtitle = "$planLabel · $status",
                icon = Icons.Filled.CreditCard,
            ) { sheet = AccountSheet.Subscription }
            if (appState.needsPaymentAttention) {
                SettingsRow(
                    title = "Reactivate access",
                    subtitle = "Subscribe again with Google Play",
                    icon = Icons.Filled.WorkspacePremium,
                ) { appState.presentPaywall(PaywallContext.Reactivate) }
            }
            SettingsRow(
                title = "Change plan",
                subtitle = "Packages billed through Google Play",
                icon = Icons.Filled.WorkspacePremium,
            ) { appState.presentPaywall(PaywallContext.ChangePlan) }
            SettingsRow(
                title = "Downloads",
                subtitle = "View offline titles",
                icon = Icons.Filled.Download,
            ) { /* Tab switch wired from MainScaffold separately */ }
        }

        SettingsGroup(title = "Privacy & Family") {
            SettingsRow(
                title = "Parental Controls",
                subtitle = if (parental.isEnabled) "On · ${parental.maturityLabel}" else "Off · age assurance via profiles",
                icon = Icons.Filled.Shield,
            ) { sheet = AccountSheet.Parental }
            SettingsRow(
                title = "Age Assurance",
                subtitle = "How we verify age · current profile: ${profile?.ageLabel ?: "—"}",
                icon = Icons.Filled.ChildCare,
            ) { sheet = AccountSheet.AgeAssurance }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("Switch\nProfile", Icons.Filled.SwapHoriz) { appState.switchProfile() }
            QuickAction("Account\nInfo", Icons.Filled.Info) { sheet = AccountSheet.AccountInfo }
            QuickAction("Playback\nHelp", Icons.Filled.PlayCircle) { sheet = AccountSheet.PlaybackHelp }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.06f)),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { appState.signOut() }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, null, tint = Color(0xFFE5484D), modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Sign Out", color = Color(0xFFE5484D), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(StColors.Border))
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { sheet = AccountSheet.DeleteAccount }
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Delete, null, tint = Color(0xFFE5484D).copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Delete Account", color = Color(0xFFE5484D).copy(alpha = 0.85f), fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        Text(
            "Story Time Universe · v1.0",
            color = StColors.Muted.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 24.dp),
        )
    }

    if (sheet != AccountSheet.None) {
        Dialog(
            onDismissRequest = { sheet = AccountSheet.None },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(Modifier.fillMaxSize().background(StColors.Background)) {
                when (sheet) {
                    AccountSheet.AccountInfo -> AccountInfoScreen(appState) { sheet = AccountSheet.None }
                    AccountSheet.Subscription -> SubscriptionInfoScreen(appState) { sheet = AccountSheet.None }
                    AccountSheet.PlaybackHelp -> PlaybackHelpScreen { sheet = AccountSheet.None }
                    AccountSheet.Parental -> ParentalControlsScreen { sheet = AccountSheet.None }
                    AccountSheet.AgeAssurance -> AgeAssuranceScreen(appState) { sheet = AccountSheet.None }
                    AccountSheet.DeleteAccount -> DeleteAccountScreen(appState) { sheet = AccountSheet.None }
                    AccountSheet.None -> Unit
                }
            }
        }
    }
}

@Composable
private fun SettingsGroup(title: String? = null, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        title?.let {
            Text(
                it,
                color = StColors.Muted,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.08f)),
        ) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = StColors.Accent, modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = StColors.Foreground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, color = StColors.Muted, fontSize = 12.sp, maxLines = 2)
            }
        }
        Text("›", color = StColors.Muted, fontSize = 16.sp)
    }
}

@Composable
private fun RowScope.QuickAction(title: String, icon: ImageVector, onClick: () -> Unit) {
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, null, tint = StColors.Accent)
        Text(
            title,
            color = StColors.Accent,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp,
            modifier = Modifier.height(28.dp),
        )
    }
}
