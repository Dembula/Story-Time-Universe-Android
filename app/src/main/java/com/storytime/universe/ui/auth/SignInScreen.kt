package com.storytime.universe.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.theme.StColors
import com.storytime.universe.ui.util.openUrl
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch

@Composable
fun SignInScreen(appState: AppState) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf(appState.bootstrapError) }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color.Transparent, StColors.Accent.copy(alpha = 0.08f), Color.Black.copy(alpha = 0.85f))
                )
            )
            .background(StColors.Background.copy(alpha = 0f)),
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
                modifier = Modifier.size(128.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text("Story Time Universe", color = StColors.Foreground, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text("Sign in to watch", color = StColors.Muted, fontSize = 14.sp)
            Spacer(Modifier.height(22.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
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
                    label = { Text("Password") },
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

                errorMessage?.let {
                    Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
                }

                Button(
                    onClick = {
                        errorMessage = null
                        scope.launch {
                            try {
                                appState.signIn(email, password)
                            } catch (e: Exception) {
                                errorMessage = e.localizedMessage ?: "Sign-in failed."
                            }
                        }
                    },
                    enabled = !appState.isBusy && email.isNotEmpty() && password.isNotEmpty(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = StColors.Accent,
                        contentColor = Color.Black,
                        disabledContainerColor = StColors.Accent.copy(alpha = 0.55f),
                        disabledContentColor = Color.Black.copy(alpha = 0.7f),
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                ) {
                    if (appState.isBusy) {
                        CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Sign In", fontWeight = FontWeight.Bold)
                    }
                }

                Text(
                    "Sign up",
                    color = StColors.AccentGold,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .clickable { openUrl(context, AppConfig.VIEWER_SIGN_UP_URL) },
                )
            }
            Spacer(Modifier.height(40.dp))
        }
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
