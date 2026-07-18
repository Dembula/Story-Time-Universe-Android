package com.storytime.universe.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.launch
import java.util.Calendar

@Composable
fun PinEntryDialog(profileName: String, onDismiss: () -> Unit, onSubmit: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StColors.Surface,
        title = { Text("Enter PIN for $profileName", color = StColors.Foreground) },
        text = {
            OutlinedTextField(
                value = pin,
                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                label = { Text("4-digit PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(pin) }, enabled = pin.length == 4) {
                Text("Continue", color = StColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = StColors.Muted) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateProfileDialog(onDismiss: () -> Unit, onCreated: () -> Unit) {
    val scope = rememberCoroutineScope()
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)

    var name by remember { mutableStateOf("") }
    var year by remember { mutableStateOf(currentYear - 21) }
    var month by remember { mutableStateOf(1) }
    var day by remember { mutableStateOf(1) }
    var usePin by remember { mutableStateOf(false) }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StColors.Surface,
        title = { Text("New Profile", color = StColors.Foreground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    NumberDropdown("Year", (1920..currentYear).toList().reversed(), year, { year = it }, Modifier.weight(1.4f))
                    NumberDropdown("Month", (1..12).toList(), month, { month = it }, Modifier.weight(1f))
                    NumberDropdown("Day", (1..31).toList(), day, { day = it }, Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Protect with PIN", color = StColors.Foreground, modifier = Modifier.weight(1f))
                    Switch(checked = usePin, onCheckedChange = { usePin = it })
                }
                if (usePin) {
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("4-digit PIN") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                error?.let { Text(it, color = Color(0xFFFF5A5A)) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    busy = true
                    error = null
                    scope.launch {
                        try {
                            ViewerApi.createProfile(
                                name = name.trim(),
                                birthYear = year,
                                birthMonth = month,
                                birthDay = day,
                                pin = if (usePin) pin else null,
                            )
                            onCreated()
                        } catch (e: Exception) {
                            error = e.localizedMessage
                        } finally {
                            busy = false
                        }
                    }
                },
                enabled = !busy && name.trim().isNotEmpty(),
            ) {
                Text("Create", color = StColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = StColors.Muted) }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NumberDropdown(
    label: String,
    options: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = selected.toString(),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.toString()) },
                    onClick = {
                        onSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}
