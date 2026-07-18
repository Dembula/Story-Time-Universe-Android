package com.storytime.universe.ui.detail

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.model.PersonPreview
import com.storytime.universe.data.model.PersonRoute
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.components.RemoteImage
import com.storytime.universe.ui.main.NavActions
import com.storytime.universe.ui.theme.StColors

@Composable
fun PersonDetailScreen(route: PersonRoute?, actions: NavActions, onBack: () -> Unit) {
    var preview by remember { mutableStateOf<PersonPreview?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(route?.id) {
        if (route == null) {
            isLoading = false
            errorMessage = "No person selected."
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null
        try {
            preview = ViewerApi.fetchPerson(route)
        } catch (e: Exception) {
            val bio = route.fallbackBio?.trim()
            if (!bio.isNullOrEmpty()) {
                preview = PersonPreview(
                    personId = route.personId ?: route.crewMemberId ?: route.fallbackName,
                    displayName = route.fallbackName,
                    roles = route.fallbackRole?.let { listOf(it) },
                    bio = bio,
                )
            } else {
                errorMessage = "Couldn't load this person's profile. ${e.localizedMessage}"
            }
        } finally {
            isLoading = false
        }
    }

    Box(Modifier.fillMaxSize().background(StColors.Background)) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            item { Spacer(Modifier.height(40.dp)) }
            item { Header(preview, route) }

            if (isLoading) {
                item { Box(Modifier.fillMaxWidth().padding(top = 40.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = StColors.Accent) } }
            } else if (preview != null) {
                val p = preview!!
                val blurb = p.blurb?.trim()
                val bio = (p.bio ?: route?.fallbackBio)?.trim()
                if (!blurb.isNullOrEmpty()) {
                    item { Text(blurb, color = StColors.Foreground.copy(alpha = 0.92f), fontSize = 15.sp) }
                } else if (!bio.isNullOrEmpty()) {
                    item { Text(bio, color = StColors.Foreground.copy(alpha = 0.92f), fontSize = 15.sp) }
                }
                if (!bio.isNullOrEmpty() && !blurb.isNullOrEmpty() && bio != blurb) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("About", color = StColors.Foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            Text(bio, color = StColors.Muted, fontSize = 15.sp)
                        }
                    }
                }
                item { StatsRow(p) }

                p.topGenres?.takeIf { it.isNotEmpty() }?.let { genres ->
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Known for", color = StColors.Foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            ChipWrap(genres)
                        }
                    }
                }

                p.credits?.takeIf { it.isNotEmpty() }?.let { credits ->
                    item { Text("Credits", color = StColors.Foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
                    credits.chunked(3).forEach { rowCredits ->
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                rowCredits.forEach { credit ->
                                    Column(
                                        Modifier.weight(1f).clickable { actions.openDetail(credit.asContentItem()) },
                                        verticalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        RemoteImage(urls = credit.posterCandidates, modifier = Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(10.dp)))
                                        Text(credit.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                        Text(credit.role, color = StColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                                repeat(3 - rowCredits.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            } else {
                errorMessage?.let { item { Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp) } }
            }
        }

        Box(
            Modifier.padding(12.dp).size(40.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
    }
}

@Composable
private fun Header(preview: PersonPreview?, route: PersonRoute?) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        val id = preview?.personId ?: route?.id ?: "x"
        Box(
            Modifier.size(88.dp).clip(CircleShape).background(
                Brush.linearGradient(listOf(StColors.Accent.copy(alpha = 0.75f), StColors.profileColor(id)))
            ),
            contentAlignment = Alignment.Center,
        ) {
            val images = preview?.imageCandidates ?: emptyList()
            if (images.isNotEmpty()) {
                RemoteImage(urls = images, modifier = Modifier.fillMaxSize().clip(CircleShape))
            } else {
                Text(preview?.initials ?: initials(route?.fallbackName ?: "?"), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(preview?.displayName ?: route?.fallbackName ?: "", color = StColors.Foreground, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                if (preview?.verified == true) Icon(Icons.Filled.Verified, null, tint = StColors.Accent, modifier = Modifier.size(20.dp))
            }
            val roles = preview?.roles?.filter { it.isNotEmpty() } ?: emptyList()
            if (roles.isNotEmpty()) {
                Text(roles.joinToString(" · "), color = StColors.AccentGold, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            } else route?.fallbackRole?.takeIf { it.isNotEmpty() }?.let {
                Text(it, color = StColors.AccentGold, fontWeight = FontWeight.Medium, fontSize = 14.sp)
            }
            (preview?.productionCount)?.takeIf { it > 0 }?.let {
                Text("$it production${if (it == 1) "" else "s"}", color = StColors.Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StatsRow(preview: PersonPreview) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        preview.followerCount?.let { StatChip("Followers", it.toString()) }
        preview.followingCount?.let { StatChip("Following", it.toString()) }
        preview.productionCount?.let { StatChip("Titles", it.toString()) }
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.StatChip(title: String, value: String) {
    Column(
        Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.06f)).padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(value, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(title, color = StColors.Muted, fontSize = 11.sp)
    }
}

@Composable
private fun ChipWrap(items: List<String>) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { item ->
                    Text(
                        item,
                        color = StColors.Foreground,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).padding(horizontal = 10.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

private fun initials(name: String): String {
    val parts = name.split(" ").filter { it.isNotEmpty() }.take(2)
    val chars = parts.mapNotNull { it.firstOrNull() }
    return if (chars.isEmpty()) name.take(1).uppercase() else chars.joinToString("").uppercase()
}
