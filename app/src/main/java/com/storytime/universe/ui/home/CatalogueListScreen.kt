package com.storytime.universe.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.model.CatalogueListRequest
import com.storytime.universe.data.model.CatalogueTypes
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.matchesGenre
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.data.parental.ParentalControls
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.main.NavActions
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CatalogueListScreen(
    request: CatalogueListRequest,
    appState: AppState,
    actions: NavActions,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parental = remember { ParentalControls.get(context) }
    val profileAge = appState.activeProfile?.age

    var items by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    suspend fun load() {
        errorMessage = null
        try {
            val combined: List<ContentItem> = when {
                request.continueWatching.isNotEmpty() ->
                    request.continueWatching.map { it.asContentItem() }

                request.genre != null -> {
                    val sample = runCatching { ViewerApi.fetchContent(limit = 80) }.getOrDefault(emptyList())
                    sample.filter { it.matchesGenre(request.genre) }
                }

                request.typeValues.isNotEmpty() || request.categoryFilter != null -> {
                    val def = CatalogueTypes.RowDefinition(
                        id = request.id,
                        typeValues = request.typeValues,
                        categoryFilter = request.categoryFilter,
                        title = request.title,
                        reserveEmptySlot = false,
                    )
                    val fetched = ViewerApi.fetchCatalogRow(def, limit = 60)
                    if (fetched.isEmpty()) request.seedItems else fetched
                }

                else -> request.seedItems
            }

            val seen = HashSet<String>()
            items = parental.filter(combined, profileAge).filter { seen.add(it.id) }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
            if (items.isEmpty()) {
                items = parental.filter(request.seedItems, profileAge)
            }
        }
    }

    LaunchedEffect(request.id, request.genre, profileAge) {
        isLoading = true
        load()
        isLoading = false
    }

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = StColors.Foreground)
            }
            Text(
                request.title,
                color = StColors.Foreground,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
        }

        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    load()
                    isRefreshing = false
                }
            },
            modifier = Modifier.fillMaxSize(),
        ) {
            when {
                isLoading && items.isEmpty() -> Box(
                    Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = StColors.Accent)
                }

                items.isEmpty() -> Column(
                    Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.Movie, null, tint = StColors.Muted, modifier = Modifier.size(48.dp))
                    Text("Nothing here yet", color = StColors.Foreground, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        errorMessage ?: "Check back when new titles arrive.",
                        color = StColors.Muted,
                        fontSize = 14.sp,
                    )
                }

                else -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(110.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    items(items, key = { it.id }) { item ->
                        PosterCard(
                            item = item,
                            modifier = Modifier.clickable { actions.openDetail(item) },
                        )
                    }
                }
            }
        }

        errorMessage?.takeIf { items.isNotEmpty() }?.let {
            Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp, modifier = Modifier.padding(16.dp))
        }
    }
}
