package com.storytime.universe.ui.auth

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.R
import com.storytime.universe.data.AppConfig
import com.storytime.universe.data.download.DownloadController
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.theme.StColors
import com.storytime.universe.ui.util.openUrl
import kotlinx.coroutines.launch

private enum class AuthMode { SIGN_IN, SIGN_UP }

@Composable
fun SignInScreen(appState: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var mode by remember { mutableStateOf(AuthMode.SIGN_IN) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var acceptedTerms by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf(appState.bootstrapError) }
    val downloadEntries by DownloadController.entries.collectAsState()
    val hasOfflineDownloads = downloadEntries.any { it.isPlayableOffline }

    val canSignUp = acceptedTerms && email.trim().isNotEmpty() && password.length >= 8

    Box(
        Modifier
            .fillMaxSize()
            .background(StColors.Background)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, StColors.Accent.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.85f))
                )
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier.size(118.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("Story Time Universe", color = StColors.Foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                if (mode == AuthMode.SIGN_IN) "Sign in to watch" else "Create your account",
                color = StColors.Muted,
                fontSize = 14.sp,
            )
            Spacer(Modifier.height(18.dp))

            // Sign In / Sign Up tabs
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.08f))
                    .padding(3.dp),
            ) {
                ModeTab("Sign In", mode == AuthMode.SIGN_IN, Modifier.weight(1f)) {
                    mode = AuthMode.SIGN_IN
                    errorMessage = null
                }
                ModeTab("Sign Up", mode == AuthMode.SIGN_UP, Modifier.weight(1f)) {
                    mode = AuthMode.SIGN_UP
                    errorMessage = null
                }
            }

            Spacer(Modifier.height(18.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (mode == AuthMode.SIGN_UP) {
                    Text(
                        "Create your account here, then choose Basic, Standard, or Premium. Payment stays in Google Play when products are live.",
                        color = StColors.Muted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name (optional)") },
                        singleLine = true,
                        colors = fieldColors(),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = {
                        Text(if (mode == AuthMode.SIGN_UP) "Password (min 8 characters)" else "Password")
                    },
                    singleLine = true,
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                contentDescription = null,
                                tint = StColors.Muted,
                            )
                        }
                    },
                    colors = fieldColors(),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (mode == AuthMode.SIGN_UP) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Switch(
                            checked = acceptedTerms,
                            onCheckedChange = { acceptedTerms = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = StColors.Accent, checkedThumbColor = Color.Black),
                        )
                        Column {
                            Text("I agree to the", color = StColors.Muted, fontSize = 12.sp)
                            Row {
                                Text(
                                    "Terms of Use",
                                    color = StColors.Accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        openUrl(context, AppConfig.TERMS_URL)
                                    },
                                )
                                Text(" and ", color = StColors.Muted, fontSize = 12.sp)
                                Text(
                                    "Privacy",
                                    color = StColors.Accent,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable {
                                        openUrl(context, AppConfig.PRIVACY_URL)
                                    },
                                )
                            }
                        }
                    }
                }

                errorMessage?.let {
                    Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                }

                Button(
                    onClick = {
                        errorMessage = null
                        scope.launch {
                            try {
                                if (mode == AuthMode.SIGN_IN) {
                                    appState.signIn(email, password)
                                } else {
                                    if (!canSignUp) {
                                        errorMessage = "Accept the Terms and enter a password of at least 8 characters."
                                        return@launch
                                    }
                                    appState.signUp(
                                        email = email,
                                        password = password,
                                        name = name.trim().ifEmpty { null },
                                    )
                                }
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Something went wrong."
                            }
                        }
                    },
                    enabled = !appState.isBusy && when (mode) {
                        AuthMode.SIGN_IN -> email.isNotEmpty() && password.isNotEmpty()
                        AuthMode.SIGN_UP -> canSignUp
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StColors.Accent,
                        contentColor = Color.Black,
                        disabledContainerColor = StColors.Accent.copy(alpha = 0.55f),
                        disabledContentColor = Color.Black.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                ) {
                    if (appState.isBusy) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (mode == AuthMode.SIGN_IN) "Sign In" else "Create Account",
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                if (mode == AuthMode.SIGN_IN) {
                    Text(
                        "Forgot password?",
                        color = StColors.Muted,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { openUrl(context, AppConfig.FORGOT_PASSWORD_URL) },
                    )
                } else {
                    Text(
                        "After your account is created you’ll pick Basic (R29.99), Standard (R89.99), or Premium (R119.99).",
                        color = StColors.Muted,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                if (hasOfflineDownloads) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Watch downloads offline",
                        color = StColors.Accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                            .clickable { appState.enterOfflineDownloads() }
                            .padding(vertical = 14.dp),
                    )
                }
            }
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ModeTab(label: String, active: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .clip(CircleShape)
            .background(if (active) StColors.Accent else Color.Transparent)
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = if (active) Color.Black else StColors.Muted, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = StColors.Foreground,
    unfocusedTextColor = StColors.Foreground,
    focusedBorderColor = StColors.Accent,
    unfocusedBorderColor = StColors.Border,
    focusedLabelColor = StColors.Muted,
    unfocusedLabelColor = StColors.Muted,
    cursorColor = StColors.Accent,
)
