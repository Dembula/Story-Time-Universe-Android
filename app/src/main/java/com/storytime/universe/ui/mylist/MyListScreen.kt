package com.storytime.universe.ui.mylist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.home.PosterCard
import com.storytime.universe.ui.main.NavActions
import com.storytime.universe.ui.theme.StColors

@Composable
fun MyListScreen(actions: NavActions) {
    var items by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            items = ViewerApi.fetchWatchlist()
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
        } finally {
            isLoading = false
        }
    }

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        Text("My List", color = StColors.Foreground, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))

        when {
            isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = StColors.Accent) }
            items.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.BookmarkBorder, null, tint = StColors.Muted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.size(12.dp))
                Text("Your list is empty", color = StColors.Foreground, fontSize = 18.sp)
                Text("Add titles to watch them later.", color = StColors.Muted, fontSize = 14.sp)
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(items, key = { it.id }) { item ->
                    PosterCard(item = item, modifier = Modifier.clickable { actions.openDetail(item) })
                }
            }
        }

        errorMessage?.let { Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp, modifier = Modifier.padding(16.dp)) }
    }
}
