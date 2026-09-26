package com.storytime.universe.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.parental.ParentalControls
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.theme.StColors

@Composable
fun AgeAssuranceScreen(appState: AppState, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val parental = remember { ParentalControls.get(context) }
    val profile = appState.activeProfile

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        SheetHeader(title = "Age Assurance", onDismiss = onDismiss)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            InfoCard {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.VerifiedUser, null, tint = StColors.AccentGold)
                    Text("How age is verified", color = StColors.AccentGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Text(
                    "Each profile is created with a date of birth. Story Time uses that age for catalogue eligibility and parental maturity limits on this device.",
                    color = StColors.Muted,
                    fontSize = 13.sp,
                )
            }

            InfoCard {
                Text("Active profile", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(profile?.name ?: "No profile selected", color = StColors.Foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Badge(profile?.ageLabel ?: "—")
                    profile?.age?.let { Badge("Age $it") }
                }
                profile?.dateOfBirth?.takeIf { it.isNotBlank() }?.let { dob ->
                    Text("Date of birth on file: ${formatDob(dob)}", color = StColors.Muted, fontSize = 12.sp)
                }
            }

            InfoCard {
                Text("Parental maturity gate", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    if (parental.isEnabled) "Enabled · ${parental.maturityLabel}"
                    else "Off — only the profile age limit applies",
                    color = StColors.Foreground,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    "Titles rated above the allowed age are hidden from Home, Search, See All, My List, and related rows while parental controls are on.",
                    color = StColors.Muted,
                    fontSize = 12.sp,
                )
            }

            InfoCard {
                Text("This device", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("Android · Story Time Universe", color = StColors.Foreground, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Text(
                    "Sign-ins and watch activity from this app are reported as Android so the platform can attribute views correctly.",
                    color = StColors.Muted,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun InfoCard(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        content()
    }
}

@Composable
private fun Badge(text: String) {
    Text(
        text,
        color = StColors.AccentGold,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(StColors.AccentSoft)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private fun formatDob(raw: String): String {
    val prefix = raw.substringBefore('T').takeIf { it.isNotEmpty() }
    return prefix ?: raw
}
