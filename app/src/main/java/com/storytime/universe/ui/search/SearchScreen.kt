package com.storytime.universe.ui.search

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.model.SearchResult
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.home.PosterCard
import com.storytime.universe.ui.main.NavActions
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.delay

@Composable
fun SearchScreen(actions: NavActions) {
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(query) {
        delay(350)
        val q = query.trim()
        if (q.length < 2) {
            results = emptyList()
            return@LaunchedEffect
        }
        isSearching = true
        errorMessage = null
        try {
            results = ViewerApi.search(q)
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
        } finally {
            isSearching = false
        }
    }

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Search titles, genres…", color = StColors.Muted) },
            leadingIcon = { Icon(Icons.Filled.Search, null, tint = StColors.Muted) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    Icon(Icons.Filled.Close, "Clear", tint = StColors.Muted, modifier = Modifier.clickable { query = ""; results = emptyList() })
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
            modifier = Modifier.fillMaxWidth().padding(16.dp),
        )

        when {
            isSearching -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = StColors.Accent)
            }
            results.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Search, null, tint = StColors.Muted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.size(12.dp))
                Text(
                    if (query.length < 2) "Search Story Time" else "No results",
                    color = StColors.Foreground,
                    fontSize = 18.sp,
                )
                Text(
                    if (query.length < 2) "Type at least 2 characters." else "Try another title or genre.",
                    color = StColors.Muted,
                    fontSize = 14.sp,
                )
            }
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            ) {
                items(results, key = { it.id }) { result ->
                    val item = result.asContentItem()
                    PosterCard(item = item, modifier = Modifier.clickable { actions.openDetail(item) })
                }
            }
        }

        errorMessage?.let { Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp, modifier = Modifier.padding(16.dp)) }
    }
}
