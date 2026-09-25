package com.storytime.universe.ui.profiles

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.R
import com.storytime.universe.data.billing.PaywallContext
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.ViewerProfile
import com.storytime.universe.data.network.ApiException
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.components.RemoteImage
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ProfilesScreen(appState: AppState) {
    val scope = rememberCoroutineScope()

    var profiles by remember { mutableStateOf<List<ViewerProfile>>(emptyList()) }
    var backdrops by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var backdropIndex by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var pinProfile by remember { mutableStateOf<ViewerProfile?>(null) }
    var selectingId by remember { mutableStateOf<String?>(null) }
    var showCreate by remember { mutableStateOf(false) }

    suspend fun load() {
        isLoading = true
        errorMessage = null
        try {
            profiles = ViewerApi.fetchProfiles()
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
            profiles = emptyList()
        }
        val featured = runCatching { ViewerApi.fetchContent(featured = true, limit = 10) }.getOrDefault(emptyList())
        val trending = runCatching { ViewerApi.fetchContent(limit = 16) }.getOrDefault(emptyList())
        val combined = if (featured.isEmpty()) trending else featured + trending
        val seen = HashSet<String>()
        backdrops = combined.filter { seen.add(it.id) && it.posterCandidates.isNotEmpty() }
            .ifEmpty { combined.take(8) }
        backdropIndex = 0
        appState.subscription = runCatching { ViewerApi.fetchSubscription() }.getOrNull()
        isLoading = false
    }

    LaunchedEffect(Unit) { load() }
    LaunchedEffect(backdrops.size) {
        if (backdrops.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000)
            if (backdrops.size <= 1) break
            backdropIndex = (backdropIndex + 1) % backdrops.size
        }
    }

    fun activate(profile: ViewerProfile, pin: String?) {
        selectingId = profile.id
        errorMessage = null
        scope.launch {
            try {
                val active = ViewerApi.activateProfile(profile.id, pin)
                pinProfile = null
                appState.selectProfile(active)
            } catch (e: Exception) {
                errorMessage = e.localizedMessage
                if (e is ApiException.PaymentRequired) {
                    pinProfile = null
                    appState.presentPaywall(PaywallContext.Reactivate)
                }
            } finally {
                selectingId = null
            }
        }
    }

    fun select(profile: ViewerProfile) {
        if (profile.pinEnabled == true) pinProfile = profile else activate(profile, null)
    }

    val current = backdrops.getOrNull(if (backdrops.isEmpty()) 0 else backdropIndex % backdrops.size)

    Box(Modifier.fillMaxSize().background(StColors.Background)) {
        // Cycling backdrop
        if (current != null) {
            RemoteImage(urls = current.posterCandidates, modifier = Modifier.fillMaxSize())
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Black.copy(alpha = 0.2f), Color.Black.copy(alpha = 0.45f), Color.Black.copy(alpha = 0.9f))
                    )
                )
        )

        Column(Modifier.fillMaxSize()) {
            // Top bar
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(painterResource(R.drawable.app_logo), null, modifier = Modifier.height(34.dp))
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { appState.signOut() }) {
                    Text("Sign Out", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.weight(1f))

            current?.let { item ->
                Column(
                    Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        item.title.uppercase(),
                        color = Color.White,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp),
                    )
                    if (!item.category.isNullOrEmpty()) {
                        Text(item.category, color = Color.White.copy(alpha = 0.85f), fontSize = 14.sp)
                    }
                }
            }

            // Profile panel
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.82f), Color.Black)
                        )
                    )
                    .padding(top = 20.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("Choose your profile", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

                when {
                    isLoading -> CircularProgressIndicator(color = StColors.Accent, modifier = Modifier.padding(20.dp))
                    profiles.isEmpty() -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No profiles yet", color = Color.White.copy(alpha = 0.75f))
                        TextButton(onClick = { showCreate = true }) {
                            Text("Create profile", color = StColors.Accent)
                        }
                    }
                    else -> LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
                    ) {
                        items(profiles, key = { it.id }) { profile ->
                            ProfileAvatar(
                                profile = profile,
                                isLoading = selectingId == profile.id,
                                enabled = selectingId == null,
                                onClick = { select(profile) },
                            )
                        }
                        item {
                            AddProfileButton { showCreate = true }
                        }
                    }
                }

                errorMessage?.let {
                    Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
                }

                if (appState.needsPaymentAttention) {
                    PaymentBanner(
                        onSubscribe = { appState.presentPaywall(PaywallContext.Reactivate) },
                        onChangePlan = { appState.presentPaywall(PaywallContext.ChangePlan) },
                    )
                }
            }
        }
    }

    pinProfile?.let { profile ->
        PinEntryDialog(
            profileName = profile.name,
            onDismiss = { pinProfile = null },
            onSubmit = { pin -> activate(profile, pin) },
        )
    }

    if (showCreate) {
        CreateProfileDialog(
            onDismiss = { showCreate = false },
            onCreated = {
                showCreate = false
                scope.launch { load() }
            },
        )
    }
}

@Composable
private fun ProfileAvatar(profile: ViewerProfile, isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .clickable(enabled = enabled) { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(StColors.profileColor(profile.id), StColors.profileColor(profile.id).copy(alpha = 0.65f))
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
            } else {
                Text(profile.name.take(1).uppercase(), color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            }
            if (profile.pinEnabled == true) {
                Box(
                    Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(6.dp),
                ) {
                    Icon(Icons.Filled.Lock, null, tint = Color.White, modifier = Modifier.size(12.dp))
                }
            }
        }
        Text(profile.name, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(profile.ageLabel, color = Color.White.copy(alpha = 0.65f), fontSize = 11.sp)
    }
}

@Composable
private fun AddProfileButton(onClick: () -> Unit) {
    Column(
        modifier = Modifier.width(100.dp).clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            Modifier
                .size(92.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Add, null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Text("Add", color = Color.White.copy(alpha = 0.8f), fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

@Composable
private fun PaymentBanner(onSubscribe: () -> Unit, onChangePlan: () -> Unit) {
    Column(
        Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StColors.AccentSoft)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("Subscription payment required", color = StColors.Foreground, fontWeight = FontWeight.Bold)
        Text(
            "Subscribe or reactivate to enter the catalogue. Choose Basic (R29.99), Standard (R89.99), Premium (R119.99), or Pay Per View (R49.99 per title · 7 days).",
            color = StColors.Muted,
            fontSize = 13.sp,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(StColors.Accent)
                .clickable { onSubscribe() }
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Choose a plan", color = Color.Black, fontWeight = FontWeight.SemiBold)
        }
        Text(
            "Switch plan",
            color = StColors.AccentGold,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .clickable { onChangePlan() },
        )
    }
}
