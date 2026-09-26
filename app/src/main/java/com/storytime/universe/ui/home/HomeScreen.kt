package com.storytime.universe.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.media.ImagePrefetcher
import com.storytime.universe.data.model.CatalogueListRequest
import com.storytime.universe.data.model.CatalogueTypes
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.ContinueWatchingItem
import com.storytime.universe.data.model.HomeBrowseFilter
import com.storytime.universe.data.model.HomeCatalogRow
import com.storytime.universe.data.model.TitleAccessResult
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.data.parental.ParentalControls
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.components.ParentalPinDialog
import com.storytime.universe.ui.main.NavActions
import com.storytime.universe.ui.player.PlaybackRequest
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(appState: AppState, actions: NavActions) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parental = remember { ParentalControls.get(context) }
    val profileAge = appState.activeProfile?.age

    var featured by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var continueWatching by remember { mutableStateOf<List<ContinueWatchingItem>>(emptyList()) }
    var trending by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var catalogRows by remember { mutableStateOf<List<HomeCatalogRow>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var browseFilter by remember { mutableStateOf<HomeBrowseFilter>(HomeBrowseFilter.All) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var pendingSwitchProfile by remember { mutableStateOf(false) }
    var pendingPlay by remember { mutableStateOf<PlaybackRequest?>(null) }

    suspend fun loadHome(force: Boolean = false) {
        if (appState.isOfflineMode) {
            isLoading = false
            errorMessage = "You're offline. Open Downloads to watch titles saved on this device."
            return
        }
        if (!force && (featured.isNotEmpty() || trending.isNotEmpty() || catalogRows.isNotEmpty())) {
            isLoading = false
            return
        }
        errorMessage = null
        val typeFilter = when (val f = browseFilter) {
            is HomeBrowseFilter.ContentType -> f.typeValues.firstOrNull()
            else -> null
        }

        val typeResults = if (typeFilter == null) {
            fetchAllTypeRows()
        } else {
            val def = CatalogueTypes.allHomeRows.firstOrNull {
                it.typeValues.any { v -> v.equals(typeFilter, ignoreCase = true) }
            } ?: CatalogueTypes.RowDefinition(typeFilter, listOf(typeFilter), title = CatalogueTypes.pluralTitle(typeFilter))
            listOf(
                HomeCatalogRow(
                    id = def.id,
                    typeValue = typeFilter,
                    title = def.title,
                    items = ViewerApi.fetchCatalogRow(def, limit = 24),
                    reserveEmptySlot = false,
                )
            )
        }

        val f = runCatching {
            ViewerApi.fetchContent(featured = true, limit = 8, type = typeFilter)
        }.getOrDefault(emptyList())
        val t = runCatching {
            ViewerApi.fetchContent(limit = 24, type = typeFilter)
        }.getOrDefault(emptyList())
        val cw = runCatching { ViewerApi.fetchContinueWatching() }.getOrDefault(emptyList())

        featured = parental.filter(if (f.isEmpty()) t.take(5) else f, profileAge)
        trending = parental.filter(t, profileAge)
        continueWatching = cw.filter { parental.allows(it.asContentItem().minAge, profileAge) }
        catalogRows = mergeDiscoveredTypes(typeResults, t + f).map { row ->
            row.copy(items = parental.filter(row.items, profileAge))
        }

        val prefetchUrls = buildList {
            featured.forEach { addAll(it.posterCandidates) }
            trending.forEach { addAll(it.posterCandidates) }
            continueWatching.forEach { addAll(it.posterCandidates) }
            catalogRows.forEach { row -> row.items.forEach { addAll(it.posterCandidates) } }
        }
        ImagePrefetcher.prefetch(context, prefetchUrls)

        if (featured.isEmpty() && trending.isEmpty() && catalogRows.all { it.items.isEmpty() }) {
            errorMessage = "Could not load the catalogue. Check your connection and try again."
        }
        isLoading = false
    }

    LaunchedEffect(appState.isOfflineMode, browseFilter, profileAge) {
        isLoading = true
        loadHome(force = true)
    }

    fun startPlay(request: PlaybackRequest) {
        if (parental.needsPinForPlayer()) {
            pendingPlay = request
            return
        }
        if (!appState.isPayPerViewAccount) {
            actions.play(request)
            return
        }
        scope.launch {
            when (val access = appState.resolvePlayAccess(request)) {
                TitleAccessResult.Playable -> actions.play(request)
                is TitleAccessResult.RequiresPurchase ->
                    appState.presentPpvUnlock(request.contentId, request.title, resume = request)
                is TitleAccessResult.Blocked -> actions.openDetailById(request.contentId)
            }
        }
    }

    fun requestPlay(contentId: String, title: String) {
        startPlay(PlaybackRequest(contentId, title))
    }

    fun openSeeAll(request: CatalogueListRequest) {
        actions.openCatalogue(request)
    }

    val populatedTypeIds = remember(catalogRows, trending, featured) {
        val ids = linkedSetOf("ALL")
        catalogRows.filter { it.shouldDisplay }.forEach { ids.add(it.id) }
        (trending + featured).mapNotNull { it.type?.uppercase() }.forEach { ids.add(it) }
        ids
    }
    val populatedGenres = remember(catalogRows, trending, featured) {
        CatalogueTypes.populatedGenres(catalogRows.flatMap { it.items } + trending + featured)
    }

    Box(Modifier.fillMaxSize().background(StColors.Background)) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    loadHome(force = true)
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(28.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp),
            ) {
                item {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            Modifier
                                .clip(CircleShape)
                                .clickable { showCategoryPicker = true }
                                .padding(end = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                browseFilter.chromeTitle,
                                color = StColors.Foreground,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                            )
                            Icon(Icons.Filled.KeyboardArrowDown, "Browse", tint = StColors.Muted)
                        }
                        Spacer(Modifier.weight(1f))
                        Box(
                            Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(StColors.profileColor(appState.activeProfile?.id ?: "x"))
                                .clickable {
                                    if (parental.needsPinToSwitchProfile()) {
                                        pendingSwitchProfile = true
                                    } else {
                                        appState.switchProfile()
                                    }
                                },
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

                if (appState.isOfflineMode) {
                    item {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { actions.openDownloads() }
                                .padding(horizontal = 18.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("Offline mode", color = StColors.Accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Catalogue needs a connection. Tap to open Downloads.", color = StColors.Muted, fontSize = 13.sp)
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
                    if (featured.isNotEmpty() && browseFilter is HomeBrowseFilter.All) {
                        item {
                            HeroCarousel(
                                items = featured,
                                onPlay = { requestPlay(it.id, it.title) },
                                onOpen = { actions.openDetail(it) },
                            )
                        }
                    }
                    if (continueWatching.isNotEmpty() && browseFilter is HomeBrowseFilter.All) {
                        item {
                            ContinueWatchingRow(
                                items = continueWatching,
                                onSelect = { requestPlay(it.id, it.title) },
                                onSeeAll = {
                                    openSeeAll(
                                        CatalogueListRequest(
                                            id = "continue",
                                            title = "Continue Watching",
                                            continueWatching = continueWatching,
                                        )
                                    )
                                },
                            )
                        }
                    }
                    if (trending.isNotEmpty()) {
                        item {
                            ContentRow(
                                title = "Trending Now",
                                items = trending,
                                onSelect = { actions.openDetail(it) },
                                onSeeAll = {
                                    openSeeAll(
                                        CatalogueListRequest(
                                            id = "trending",
                                            title = "Trending Now",
                                            seedItems = trending,
                                        )
                                    )
                                },
                            )
                        }
                    }
                    items(catalogRows.filter { it.shouldDisplay }, key = { it.id }) { row ->
                        ContentRow(
                            title = row.title,
                            items = row.items,
                            onSelect = { actions.openDetail(it) },
                            onSeeAll = {
                                val def = CatalogueTypes.allHomeRows.firstOrNull { it.id == row.id }
                                openSeeAll(
                                    CatalogueListRequest(
                                        id = row.id,
                                        title = row.title,
                                        typeValues = def?.typeValues ?: listOf(row.typeValue),
                                        categoryFilter = def?.categoryFilter,
                                        seedItems = row.items,
                                    )
                                )
                            },
                        )
                    }
                }

                errorMessage?.let { msg ->
                    item {
                        Text(
                            msg,
                            color = Color(0xFFFF5A5A).copy(alpha = 0.9f),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 20.dp),
                        )
                    }
                }
            }
        }

        if (showCategoryPicker) {
            HomeCategoryPickerOverlay(
                filter = browseFilter,
                populatedTypeIds = populatedTypeIds,
                populatedGenres = populatedGenres,
                onSelectType = { filter ->
                    browseFilter = filter
                    showCategoryPicker = false
                },
                onSelectGenre = { genre ->
                    showCategoryPicker = false
                    openSeeAll(
                        CatalogueListRequest(
                            id = "genre-$genre",
                            title = genre,
                            genre = genre,
                        )
                    )
                },
                onDismiss = { showCategoryPicker = false },
            )
        }

        if (pendingSwitchProfile) {
            ParentalPinDialog(
                title = "Switch profile",
                message = "Enter your parental PIN to switch profiles.",
                onDismiss = { pendingSwitchProfile = false },
                onSuccess = {
                    pendingSwitchProfile = false
                    appState.switchProfile()
                },
            )
        }

        pendingPlay?.let { req ->
            ParentalPinDialog(
                title = "Play title",
                message = "Enter your parental PIN to play.",
                onDismiss = { pendingPlay = null },
                onSuccess = {
                    val playReq = pendingPlay
                    pendingPlay = null
                    if (playReq != null) {
                        if (!appState.isPayPerViewAccount) {
                            actions.play(playReq)
                        } else {
                            scope.launch {
                                when (val access = appState.resolvePlayAccess(playReq)) {
                                    TitleAccessResult.Playable -> actions.play(playReq)
                                    is TitleAccessResult.RequiresPurchase ->
                                        appState.presentPpvUnlock(playReq.contentId, playReq.title, resume = playReq)
                                    is TitleAccessResult.Blocked -> actions.openDetailById(playReq.contentId)
                                }
                            }
                        }
                    }
                },
            )
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
