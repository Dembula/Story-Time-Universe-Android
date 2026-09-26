package com.storytime.universe.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.billing.StoreProducts
import com.storytime.universe.data.model.ViewerAccountDetails
import com.storytime.universe.data.model.ViewerSettingsProfile
import com.storytime.universe.data.model.ViewerSettingsResponse
import com.storytime.universe.data.model.ViewerSettingsSubscription
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.theme.StColors

@Composable
fun AccountInfoScreen(appState: AppState, onDismiss: () -> Unit) {
    var settings by remember { mutableStateOf<ViewerSettingsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        val loaded = runCatching { ViewerApi.fetchViewerSettings() }.getOrNull()
        if (loaded != null) {
            settings = loaded
            errorMessage = null
        } else {
            errorMessage = "Couldn’t refresh full details. Showing what we have on this device."
            settings = ViewerSettingsResponse(
                account = ViewerAccountDetails(
                    name = appState.session?.user?.name,
                    email = appState.session?.user?.email,
                ),
                profiles = appState.activeProfile?.let {
                    listOf(
                        ViewerSettingsProfile(
                            id = it.id,
                            name = it.name,
                            age = it.age,
                            dateOfBirth = it.dateOfBirth,
                            pinEnabled = it.pinEnabled,
                        ),
                    )
                },
                activeProfileId = appState.activeProfile?.id,
                subscription = appState.subscription?.let {
                    ViewerSettingsSubscription(
                        id = it.id,
                        plan = it.plan,
                        viewerModel = it.viewerModel,
                        status = it.status,
                        currentPeriodEnd = it.currentPeriodEnd,
                    )
                },
            )
        }
        if (appState.subscription == null) {
            appState.refreshSubscription()
        }
        isLoading = false
    }

    val displayName = settings?.account?.name?.takeIf { it.isNotBlank() }
        ?: appState.session?.user?.name
        ?: appState.activeProfile?.name
        ?: "—"
    val displayEmail = settings?.account?.email?.takeIf { it.isNotBlank() }
        ?: appState.session?.user?.email
        ?: "—"
    val displayPhone = settings?.account?.phoneNumber?.takeIf { it.isNotBlank() } ?: "Not provided"
    val addressLines = settings?.address?.formattedLines.orEmpty()

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        SheetHeader(title = "Account info", onDismiss = onDismiss)

        when {
            isLoading && settings == null -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = StColors.Accent)
            }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.10f), Color.White.copy(alpha = 0.05f)),
                            ),
                        )
                        .border(1.dp, StColors.Border, RoundedCornerShape(18.dp))
                        .padding(18.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        StColors.profileColor(appState.activeProfile?.id ?: "a"),
                                        StColors.Accent.copy(alpha = 0.75f),
                                    ),
                                ),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            displayName.take(1).uppercase(),
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(displayName, color = StColors.Foreground, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(displayEmail, color = StColors.Muted, fontSize = 13.sp, maxLines = 1)
                        appState.activeProfile?.let {
                            Text(
                                "Watching as ${it.name} · ${it.ageLabel}",
                                color = StColors.AccentGold,
                                fontSize = 12.sp,
                            )
                        }
                    }
                }

                errorMessage?.let {
                    Text(it, color = Color(0xFFFF5A5A).copy(alpha = 0.9f), fontSize = 12.sp)
                }

                SectionCard("Personal details", Icons.Filled.Person) {
                    InfoRow("Full name", displayName)
                    InfoRow("Email", displayEmail)
                    InfoRow("Phone number", displayPhone)
                }

                SectionCard("Address", Icons.Filled.Home) {
                    if (addressLines.isEmpty()) {
                        Text("No address on file yet.", color = StColors.Muted, fontSize = 13.sp)
                    } else {
                        addressLines.forEach { line ->
                            Text(line, color = StColors.Foreground, fontSize = 15.sp)
                        }
                    }
                }

                SectionCard("Subscription", Icons.Filled.CreditCard) {
                    InfoRow(
                        "Plan",
                        StoreProducts.displayNameForPlanCode(
                            settings?.subscription?.plan ?: appState.subscription?.plan,
                        ).ifBlank {
                            if (appState.isPayPerViewAccount) "Pay Per View" else "No plan"
                        },
                    )
                    InfoRow(
                        "Status",
                        settings?.subscription?.status
                            ?: appState.subscription?.status
                            ?: "—",
                    )
                    modelLabel(settings?.subscription?.viewerModel ?: appState.subscription?.viewerModel)?.let {
                        InfoRow("Account type", it)
                    }
                    appState.subscription?.deviceCount?.let { InfoRow("Devices", "$it") }
                    appState.subscription?.profileLimit?.let { InfoRow("Profile limit", "$it") }
                }

                settings?.paymentMethods?.takeIf { it.isNotEmpty() }?.let { methods ->
                    SectionCard("Payment methods", Icons.Filled.CreditCard) {
                        methods.forEach { method ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(method.label?.takeIf { it.isNotBlank() } ?: "Card", color = StColors.Foreground, fontWeight = FontWeight.Medium)
                                    method.lastFour?.takeIf { it.isNotBlank() }?.let {
                                        Text("•••• $it", color = StColors.Muted, fontSize = 12.sp)
                                    }
                                }
                                if (method.isDefault == true) {
                                    Text(
                                        "Default",
                                        color = StColors.AccentGold,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(StColors.AccentSoft)
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                settings?.profiles?.takeIf { it.isNotEmpty() }?.let { profiles ->
                    SectionCard("Profiles", Icons.Filled.People) {
                        profiles.forEach { profile ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(StColors.profileColor(profile.id)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        (profile.name ?: "?").take(1).uppercase(),
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                    )
                                }
                                Column(Modifier.weight(1f)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Text(profile.name ?: "Profile", color = StColors.Foreground, fontWeight = FontWeight.SemiBold)
                                        if (profile.id == settings?.activeProfileId || profile.id == appState.activeProfile?.id) {
                                            Text("Active", color = StColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    Text(profileAgeLabel(profile), color = StColors.Muted, fontSize = 12.sp)
                                }
                                if (profile.pinEnabled == true) {
                                    Icon(Icons.Filled.Lock, null, tint = StColors.Muted, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                    }
                }

                settings?.preferences?.let { prefs ->
                    SectionCard("Preferences", Icons.Filled.Tune) {
                        InfoRow("Email notifications", if (prefs.notifyEmail != false) "On" else "Off")
                        InfoRow("Playback quality", (prefs.playbackQuality ?: "auto").replaceFirstChar { it.uppercase() })
                    }
                }

                Text(
                    "This is a read-only summary of what you set up on Story Time. Contact support if something needs updating.",
                    color = StColors.Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(bottom = 24.dp),
                )
            }
        }
    }
}

@Composable
internal fun SheetHeader(title: String, onDismiss: () -> Unit, dismissLabel: String = "Done") {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextButton(onClick = onDismiss) {
            Text(dismissLabel, color = StColors.Accent)
        }
        Text(
            title,
            color = StColors.Foreground,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.size(72.dp))
    }
}

@Composable
internal fun SectionCard(title: String, icon: ImageVector, content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = StColors.AccentGold, modifier = Modifier.size(16.dp))
            Text(title, color = StColors.AccentGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        content()
    }
}

@Composable
internal fun InfoRow(label: String, value: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(label, color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Text(value, color = StColors.Foreground, fontSize = 15.sp)
    }
}

private fun profileAgeLabel(profile: ViewerSettingsProfile): String {
    val age = profile.age ?: return "Profile"
    val band = when {
        age <= 12 -> "Kids"
        age <= 15 -> "Teen"
        else -> "Adult"
    }
    return "$band · Age $age"
}

private fun modelLabel(raw: String?): String? {
    val value = raw?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val upper = value.uppercase()
    if ("PPV" in upper || "PAY" in upper) return "Pay Per View"
    return value.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }
}
