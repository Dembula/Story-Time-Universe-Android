package com.storytime.universe.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.SearchResult
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.data.parental.ParentalControls
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.components.RemoteImage
import com.storytime.universe.ui.home.PosterCard
import com.storytime.universe.ui.main.NavActions
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(appState: AppState, actions: NavActions) {
    val context = LocalContext.current
    val parental = remember { ParentalControls.get(context) }
    val profileAge = appState.activeProfile?.age

    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var recommended by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var isLoadingRecommended by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAiSearch by remember { mutableStateOf(false) }

    LaunchedEffect(profileAge) {
        isLoadingRecommended = true
        val featured = runCatching { ViewerApi.fetchContent(featured = true, limit = 12) }.getOrDefault(emptyList())
        val trending = runCatching { ViewerApi.fetchContent(limit = 24) }.getOrDefault(emptyList())
        val seen = HashSet<String>()
        val merged = (featured + trending).filter { seen.add(it.id) }
        recommended = parental.filter(merged, profileAge)
        isLoadingRecommended = false
    }

    LaunchedEffect(query, profileAge) {
        delay(350)
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        isSearching = true
        errorMessage = null
        try {
            val raw = ViewerApi.search(q)
            val allowed = parental.filter(raw.map { it.asContentItem() }, profileAge).map { it.id }.toSet()
            results = raw.filter { it.id in allowed }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
        } finally {
            isSearching = false
        }
    }

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 16.dp, top = 12.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Search",
                color = StColors.Foreground,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(StColors.Accent.copy(alpha = 0.14f))
                    .clickable { showAiSearch = true }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = StColors.Accent, modifier = Modifier.size(16.dp))
                Text("AI", color = StColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search titles, genres…", color = StColors.Muted) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = StColors.Muted) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear",
                        tint = StColors.Muted,
                        modifier = Modifier.clickable {
                            query = ""
                            results = emptyList()
                        },
                    )
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = StColors.Card,
                unfocusedContainerColor = StColors.Card,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = StColors.Foreground,
                unfocusedTextColor = StColors.Foreground,
                cursorColor = StColors.Accent,
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        )

        when {
            isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StColors.Accent)
            }

            query.trim().length >= 2 -> {
                if (results.isEmpty()) {
                    EmptySearchState(
                        title = "No results",
                        subtitle = "Try another title or genre.",
                    )
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(110.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(results, key = { it.id }) { result ->
                            val item = result.asContentItem()
                            PosterCard(item = item, modifier = Modifier.clickable { actions.openDetail(item) })
                        }
                    }
                }
            }

            isLoadingRecommended && recommended.isEmpty() -> Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = StColors.Accent)
            }

            recommended.isEmpty() -> EmptySearchState(
                title = "Search Story Time",
                subtitle = "Type at least 2 characters, or browse recommended titles below.",
            )

            else -> LazyColumn(Modifier.fillMaxSize()) {
                item {
                    Text(
                        "Recommended series & films",
                        color = StColors.Foreground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(recommended, key = { it.id }) { item ->
                    RecommendedRow(item = item, onClick = { actions.openDetail(item) })
                }
                item { Spacer(Modifier.height(28.dp)) }
            }
        }

        errorMessage?.let {
            Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }
    }

    if (showAiSearch) {
        Dialog(
            onDismissRequest = { showAiSearch = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) {
            Box(Modifier.fillMaxSize().background(StColors.Background)) {
                AISearchScreen(
                    appState = appState,
                    onOpenDetail = { item ->
                        showAiSearch = false
                        actions.openDetail(item)
                    },
                    onDismiss = { showAiSearch = false },
                )
            }
        }
    }
}

@Composable
private fun EmptySearchState(title: String, subtitle: String) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Filled.Search, null, tint = StColors.Muted, modifier = Modifier.size(48.dp))
        Spacer(Modifier.size(12.dp))
        Text(title, color = StColors.Foreground, fontSize = 18.sp)
        Text(subtitle, color = StColors.Muted, fontSize = 14.sp)
    }
}

@Composable
private fun RecommendedRow(item: ContentItem, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        RemoteImage(
            urls = item.posterCandidates,
            modifier = Modifier
                .width(64.dp)
                .height(96.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                item.title,
                color = StColors.Foreground,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(item.displayType, color = StColors.Muted, fontSize = 13.sp, maxLines = 1)
        }
        Icon(Icons.Filled.PlayCircle, contentDescription = null, tint = StColors.Accent, modifier = Modifier.size(28.dp))
    }
}
