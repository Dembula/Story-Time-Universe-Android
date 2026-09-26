package com.storytime.universe.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.data.parental.ParentalControls
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.delay

private data class MaturityOption(val age: Int, val label: String, val description: String)

@Composable
fun ParentalControlsScreen(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val parental = remember { ParentalControls.get(context) }

    var isEnabled by remember { mutableStateOf(parental.isEnabled) }
    var maxAge by remember { mutableIntStateOf(parental.maxMaturityAge) }
    var requirePinSwitch by remember { mutableStateOf(parental.requirePinToSwitchProfile) }
    var requirePinPlayer by remember { mutableStateOf(parental.requirePinForPlayer) }
    var blockDownloads by remember { mutableStateOf(parental.blockDownloads) }
    var hasPin by remember { mutableStateOf(parental.hasPin) }
    var unlocked by remember { mutableStateOf(!parental.hasPin) }
    var unlockPin by remember { mutableStateOf("") }
    var unlockError by remember { mutableStateOf<String?>(null) }

    var newPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf<String?>(null) }
    var pinSuccess by remember { mutableStateOf<String?>(null) }
    var remoteHint by remember { mutableStateOf<String?>(null) }

    val maturityOptions = remember {
        listOf(
            MaturityOption(7, "Little Kids", "Ages 7 and under"),
            MaturityOption(12, "Kids", "Ages 12 and under"),
            MaturityOption(15, "Teens", "Ages 15 and under"),
            MaturityOption(17, "Young Adults", "Ages 17 and under"),
            MaturityOption(18, "All Content", "No restrictions"),
        )
    }

    LaunchedEffect(Unit) {
        val settings = runCatching { ViewerApi.fetchViewerSettings() }.getOrNull()
        val prefs = settings?.preferences
        if (prefs != null) {
            parental.applyRemoteMaturityHints(
                enabled = prefs.parentalControlsEnabled,
                maxAge = prefs.resolvedMaxMaturityAge,
            )
            isEnabled = parental.isEnabled
            maxAge = parental.maxMaturityAge
            if (prefs.parentalControlsEnabled != null || prefs.resolvedMaxMaturityAge != null) {
                remoteHint = "Synced maturity hints from your Story Time account preferences."
            }
        }
    }

    LaunchedEffect(pinSuccess) {
        if (pinSuccess != null) {
            delay(3000)
            pinSuccess = null
        }
    }

    fun persistEnabled(value: Boolean) {
        isEnabled = value
        parental.isEnabled = value
    }

    fun persistMaxAge(value: Int) {
        maxAge = value
        parental.maxMaturityAge = value
    }

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        SheetHeader(title = "Parental Controls", onDismiss = onDismiss)

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            if (hasPin && !unlocked) {
                Column(
                    Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Icon(Icons.Filled.Shield, null, tint = StColors.Accent, modifier = Modifier.size(48.dp).padding(top = 24.dp))
                    Text(
                        "Enter your PIN to manage parental controls",
                        color = StColors.Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedTextField(
                        value = unlockPin,
                        onValueChange = {
                            if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                                unlockPin = it
                                unlockError = null
                                if (it.length == 4) {
                                    if (parental.verifyPin(it)) {
                                        unlocked = true
                                    } else {
                                        unlockError = "Incorrect PIN"
                                        unlockPin = ""
                                    }
                                }
                            }
                        },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    unlockError?.let { Text(it, color = Color(0xFFFF5A5A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                }
            } else {
                remoteHint?.let {
                    Text(it, color = StColors.AccentGold, fontSize = 12.sp)
                }

                CardSection {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Filled.Shield, null, tint = StColors.Accent)
                        Column(Modifier.weight(1f)) {
                            Text("Parental Controls", color = StColors.Foreground, fontWeight = FontWeight.Bold)
                            Text(
                                if (isEnabled) "Active — content is filtered" else "Off — all content visible",
                                color = if (isEnabled) Color(0xFF3DDC84) else StColors.Muted,
                                fontSize = 12.sp,
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { persistEnabled(it) },
                            colors = SwitchDefaults.colors(checkedTrackColor = StColors.Accent),
                        )
                    }
                    Text(
                        "When enabled, titles above the maturity limit are hidden from Home, Search, My List, and all browsing areas.",
                        color = StColors.Muted,
                        fontSize = 12.sp,
                    )
                }

                if (isEnabled) {
                    CardSection {
                        Text("Maturity Limit", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        maturityOptions.forEach { option ->
                            val selected = maxAge == option.age
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .clickable { persistMaxAge(option.age) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(option.label, color = StColors.Foreground, fontWeight = FontWeight.Medium)
                                    Text(option.description, color = StColors.Muted, fontSize = 12.sp)
                                }
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = if (selected) StColors.Accent else StColors.Muted.copy(alpha = 0.35f),
                                )
                            }
                        }
                    }

                    CardSection {
                        Text("Restrictions", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        RestrictionToggle("Require PIN to switch profiles", Icons.Filled.People, requirePinSwitch) {
                            requirePinSwitch = it
                            parental.requirePinToSwitchProfile = it
                        }
                        RestrictionToggle("Require PIN before playback", Icons.Filled.PlayArrow, requirePinPlayer) {
                            requirePinPlayer = it
                            parental.requirePinForPlayer = it
                        }
                        RestrictionToggle("Block new downloads", Icons.Filled.Download, blockDownloads) {
                            blockDownloads = it
                            parental.blockDownloads = it
                        }
                    }
                }

                CardSection {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Device PIN", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                        if (hasPin) {
                            Text("Active", color = Color(0xFF3DDC84), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Text(
                        if (hasPin) "Your 4-digit PIN protects parental settings on this device."
                        else "Set a 4-digit PIN to lock parental settings on this device.",
                        color = StColors.Muted,
                        fontSize = 12.sp,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newPin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                            label = { Text(if (hasPin) "New PIN" else "PIN") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = confirmPin,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) confirmPin = it },
                            label = { Text("Confirm") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            modifier = Modifier.weight(1f),
                        )
                    }
                    pinError?.let { Text(it, color = Color(0xFFFF5A5A), fontSize = 12.sp) }
                    pinSuccess?.let { Text(it, color = Color(0xFF3DDC84), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }

                    if (hasPin) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                "Update PIN",
                                color = StColors.Accent,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(StColors.Accent.copy(alpha = 0.15f))
                                    .clickable {
                                        pinError = null
                                        pinSuccess = null
                                        when {
                                            newPin.isEmpty() -> pinError = "Enter a new PIN"
                                            newPin.length != 4 || !newPin.all { it.isDigit() } ->
                                                pinError = "PIN must be exactly 4 digits"
                                            newPin != confirmPin -> pinError = "PINs don't match"
                                            else -> {
                                                parental.setPin(newPin)
                                                hasPin = true
                                                newPin = ""
                                                confirmPin = ""
                                                pinSuccess = "PIN updated successfully"
                                            }
                                        }
                                    }
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                            )
                            Text(
                                "Remove PIN",
                                color = Color(0xFFE5484D),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE5484D).copy(alpha = 0.12f))
                                    .clickable {
                                        parental.clearPin()
                                        hasPin = false
                                        newPin = ""
                                        confirmPin = ""
                                        pinError = null
                                        pinSuccess = "PIN removed"
                                    }
                                    .padding(vertical = 10.dp)
                                    .fillMaxWidth(),
                            )
                        }
                    } else {
                        Text(
                            "Set PIN",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(StColors.Accent)
                                .clickable {
                                    pinError = null
                                    pinSuccess = null
                                    when {
                                        newPin.length != 4 || !newPin.all { it.isDigit() } ->
                                            pinError = "PIN must be exactly 4 digits"
                                        newPin != confirmPin -> pinError = "PINs don't match"
                                        else -> {
                                            parental.setPin(newPin)
                                            hasPin = true
                                            newPin = ""
                                            confirmPin = ""
                                            pinSuccess = "PIN saved successfully"
                                        }
                                    }
                                }
                                .padding(vertical = 12.dp),
                        )
                    }
                    Text("PIN is stored on this device only and does not sync.", color = StColors.Muted.copy(alpha = 0.7f), fontSize = 11.sp)
                }

                CardSection {
                    Text("Age Assurance", color = StColors.AccentGold, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Each viewer profile is created with a date of birth. Kids, Teen, and Adult labels are derived from the profile age and used for age assurance throughout the app.",
                        color = StColors.Muted,
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun CardSection(content: @Composable () -> Unit) {
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
private fun RestrictionToggle(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = StColors.Accent, modifier = Modifier.size(20.dp))
        Text(title, color = StColors.Foreground, fontSize = 15.sp, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = StColors.Accent),
        )
    }
}
