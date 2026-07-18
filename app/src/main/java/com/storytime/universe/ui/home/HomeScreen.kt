package com.storytime.universe.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.model.CatalogueTypes
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.ContinueWatchingItem
import com.storytime.universe.data.model.HomeCatalogRow
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.main.NavActions
import com.storytime.universe.ui.player.PlaybackRequest
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

@Composable
fun HomeScreen(appState: AppState, actions: NavActions) {
    var featured by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var continueWatching by remember { mutableStateOf<List<ContinueWatchingItem>>(emptyList()) }
    var trending by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var catalogRows by remember { mutableStateOf<List<HomeCatalogRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        if (featured.isNotEmpty() || trending.isNotEmpty() || catalogRows.isNotEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        errorMessage = null

        val typeResults = fetchAllTypeRows()
        val f = runCatching { ViewerApi.fetchContent(featured = true, limit = 8) }.getOrDefault(emptyList())
        val t = runCatching { ViewerApi.fetchContent(limit = 24) }.getOrDefault(emptyList())
        val cw = runCatching { ViewerApi.fetchContinueWatching() }.getOrDefault(emptyList())

        featured = if (f.isEmpty()) t.take(5) else f
        trending = t
        continueWatching = cw
        catalogRows = mergeDiscoveredTypes(typeResults, t + f)

        if (featured.isEmpty() && trending.isEmpty() && catalogRows.all { it.items.isEmpty() }) {
            errorMessage = "Could not load the catalogue. Pull to refresh."
        }
        isLoading = false
    }

    Box(Modifier.fillMaxSize().background(StColors.Background)) {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(28.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
        ) {
            item {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Home", color = StColors.Foreground, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(StColors.profileColor(appState.activeProfile?.id ?: "x"))
                            .clickable { appState.switchProfile() },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            (appState.activeProfile?.name ?: "?").take(1).uppercase(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            if (isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = StColors.Accent)
                    }
                }
            } else {
                if (featured.isNotEmpty()) {
                    item {
                        HeroCarousel(
                            items = featured,
                            onPlay = { actions.play(PlaybackRequest(it.id, it.title)) },
                            onOpen = { actions.openDetail(it) },
                        )
                    }
                }
                if (continueWatching.isNotEmpty()) {
                    item {
                        ContinueWatchingRow(continueWatching) { item ->
                            actions.play(PlaybackRequest(item.id, item.title))
                        }
                    }
                }
                item {
                    ContentRow("Trending Now", trending) { actions.openDetail(it) }
                }
                items(catalogRows.filter { it.shouldDisplay }, key = { it.id }) { row ->
                    ContentRow(row.title, row.items) { actions.openDetail(it) }
                }
            }

            errorMessage?.let { msg ->
                item {
                    Text(msg, color = Color(0xFFFF5A5A).copy(alpha = 0.9f), fontSize = 13.sp, modifier = Modifier.padding(horizontal = 20.dp))
                }
            }
        }
    }
}

private suspend fun fetchAllTypeRows(): List<HomeCatalogRow> = coroutineScope {
    CatalogueTypes.allHomeRows.map { def ->
        async {
            val items = ViewerApi.fetchCatalogRow(def, limit = 16)
            HomeCatalogRow(
                id = def.id,
                typeValue = def.typeValues.firstOrNull() ?: def.id,
                title = def.title,
                items = items,
                reserveEmptySlot = def.reserveEmptySlot,
            )
        }
    }.awaitAll()
}

/** If the API returns a type we don't list yet, add a row automatically (iOS parity). */
private fun mergeDiscoveredTypes(knownRows: List<HomeCatalogRow>, sample: List<ContentItem>): List<HomeCatalogRow> {
    val rows = knownRows.toMutableList()
    val known = CatalogueTypes.allTrackedTypeValues
    val extras = LinkedHashMap<String, MutableList<ContentItem>>()

    for (item in sample) {
        val raw = item.type?.trim().orEmpty()
        if (raw.isEmpty()) continue
        val key = raw.uppercase()
        if (known.contains(key)) continue
        extras.getOrPut(key) { mutableListOf() }.add(item)
    }

    for ((typeValue, items) in extras.toSortedMap()) {
        rows.add(
            HomeCatalogRow(
                id = typeValue,
                typeValue = typeValue,
                title = CatalogueTypes.pluralTitle(typeValue),
                items = items.take(16),
                reserveEmptySlot = false,
            )
        )
    }
    return rows
}
