package com.storytime.universe.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.ui.theme.StColors
import com.storytime.universe.ui.util.openUrl

@Composable
fun PlaybackHelpScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val supportEmail = "support@story-time.online"
    val supportPhoneDisplay = "+27 61 657 2691"
    val supportPhoneTel = "+27616572691"

    val steps = listOf(
        "Close the app and reopen it" to
            "Fully swipe the app away from Recents, then open Story Time Universe again.",
        "Check your Wi‑Fi or mobile data" to
            "Make sure you have a stable connection. Try switching between Wi‑Fi and cellular if playback still stalls.",
        "Try playing again" to
            "Return to the title and press Play. If this is a pay‑per‑view title, complete unlock when prompted.",
        "Use a download when you’re offline" to
            "If you saved the title earlier, open Downloads and play it offline without streaming.",
        "Check silent mode and volume" to
            "Raise the device volume and try unplugging and re-plugging headphones.",
    )

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        SheetHeader(title = "Playback Help", onDismiss = onDismiss)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PlayCircle, null, tint = StColors.AccentGold)
                    Text("Having trouble watching?", color = StColors.AccentGold, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "Most playback issues clear with a quick restart and a solid internet connection. Work through the steps below, then contact us if it still fails.",
                    color = StColors.Muted,
                    fontSize = 13.sp,
                )
            }

            Text("Try these steps", color = StColors.Foreground, fontSize = 16.sp, fontWeight = FontWeight.Bold)

            steps.forEachIndexed { index, (title, detail) ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.06f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StColors.AccentSoft),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${index + 1}", color = StColors.Accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(title, color = StColors.Foreground, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(detail, color = StColors.Muted, fontSize = 12.sp)
                    }
                }
            }

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text("Story Time Team", color = StColors.AccentGold, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text("Still stuck? We’re happy to help.", color = StColors.Muted, fontSize = 13.sp)

                ContactRow(
                    icon = Icons.Filled.Email,
                    title = "Email",
                    value = supportEmail,
                    onClick = { openUrl(context, "mailto:$supportEmail") },
                )
                ContactRow(
                    icon = Icons.Filled.Phone,
                    title = "Phone / WhatsApp",
                    value = supportPhoneDisplay,
                    onClick = { openUrl(context, "tel:$supportPhoneTel") },
                )
            }
        }
    }
}

@Composable
private fun ContactRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(onClick = onClick)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = StColors.Accent, modifier = Modifier.size(20.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(value, color = StColors.Foreground, fontSize = 15.sp, fontWeight = FontWeight.Medium)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, null, tint = StColors.Muted, modifier = Modifier.size(14.dp))
    }
}
