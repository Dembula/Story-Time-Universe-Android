package com.storytime.universe.ui.account

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.launch

@Composable
fun DeleteAccountScreen(appState: AppState, onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isWorking by remember { mutableStateOf(false) }

    val canDelete = !isWorking && password.isNotEmpty() && confirmation.uppercase() == "DELETE"

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        SheetHeader(title = "Delete Account", onDismiss = onDismiss, dismissLabel = "Cancel")

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "This permanently deletes your Story Time account and associated data. This cannot be undone.",
                color = StColors.Muted,
                fontSize = 13.sp,
            )

            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Confirm", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Account password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = confirmation,
                    onValueChange = { confirmation = it },
                    label = { Text("Type DELETE to confirm") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            errorMessage?.let {
                Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp)
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (canDelete) Color(0xFFE5484D) else Color(0xFFE5484D).copy(alpha = 0.35f))
                    .clickable(enabled = canDelete) {
                        scope.launch {
                            errorMessage = null
                            isWorking = true
                            try {
                                ViewerApi.deleteAccount(password)
                                onDismiss()
                                appState.signOut()
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Could not delete account."
                            } finally {
                                isWorking = false
                            }
                        }
                    }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (isWorking) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.padding(4.dp))
                } else {
                    Text(
                        "Delete Account Permanently",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}
