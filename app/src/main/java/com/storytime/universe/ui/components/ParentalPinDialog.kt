package com.storytime.universe.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.storytime.universe.data.parental.ParentalControls
import com.storytime.universe.ui.theme.StColors

/**
 * Device-local parental PIN entry (PIN is not synced from the web).
 * Mirrors iOS `ParentalPINSheet`.
 */
@Composable
fun ParentalPinDialog(
    title: String = "Enter PIN",
    message: String = "Enter your 4-digit parental PIN to continue.",
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
) {
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StColors.Surface,
        title = { Text(title, color = StColors.Foreground) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(message, color = StColors.Muted)
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { c -> c.isDigit() }) {
                            pin = it
                            errorMessage = null
                        }
                    },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth(),
                )
                errorMessage?.let {
                    Text(it, color = Color(0xFFFF5A5A))
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (ParentalControls.get(context).verifyPin(pin)) {
                        onSuccess()
                    } else {
                        errorMessage = "Incorrect PIN."
                        pin = ""
                    }
                },
                enabled = pin.length == 4,
            ) {
                Text("Confirm", color = StColors.Accent)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = StColors.Muted)
            }
        },
    )
}
