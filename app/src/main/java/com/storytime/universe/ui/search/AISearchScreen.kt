package com.storytime.universe.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.SearchResult
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.data.parental.ParentalControls
import com.storytime.universe.ui.AppState
import com.storytime.universe.ui.components.RemoteImage
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.launch
import java.util.UUID

private data class AIChatTurn(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val report: String? = null,
    val moodTags: List<String> = emptyList(),
    val results: List<SearchResult> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val error: String? = null,
)

@Composable
fun AISearchScreen(
    appState: AppState,
    onOpenDetail: (ContentItem) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val parental = remember { ParentalControls.get(context) }
    val profileAge = appState.activeProfile?.age
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val turns = remember { mutableStateListOf<AIChatTurn>() }

    val starterPrompts = remember {
        listOf(
            "Feel-good comedy for a rainy night",
            "Something intense for late night",
            "Family night picks",
            "A quick watch under an hour",
            "Documentary that feels real",
            "Cozy and comforting",
        )
    }

    fun canSend(text: String = query): Boolean =
        text.trim().length >= 2 && !isSearching

    fun filtered(results: List<SearchResult>): List<SearchResult> {
        val allowed = parental.filter(results.map { it.asContentItem() }, profileAge).map { it.id }.toSet()
        return results.filter { it.id in allowed }
    }

    fun send(raw: String) {
        val prompt = raw.trim()
        if (!canSend(prompt)) return
        query = ""
        isSearching = true
        val turn = AIChatTurn(prompt = prompt)
        turns.add(turn)
        val turnId = turn.id

        scope.launch {
            try {
                val response = ViewerApi.aiSearch(prompt)
                val report = response.reasoning?.trim()?.takeIf { it.isNotEmpty() }
                    ?: "Showing the closest matches from the Story Time catalogue."
                val index = turns.indexOfFirst { it.id == turnId }
                if (index >= 0) {
                    turns[index] = turns[index].copy(
                        report = report,
                        moodTags = moodChips(prompt),
                        results = response.results,
                        suggestions = response.suggestions,
                    )
                }
            } catch (e: Exception) {
                val index = turns.indexOfFirst { it.id == turnId }
                if (index >= 0) {
                    turns[index] = turns[index].copy(
                        error = e.localizedMessage,
                        report = "I hit a snag while thinking that through. Try again in a moment.",
                    )
                }
            } finally {
                isSearching = false
            }
        }
    }

    LaunchedEffect(turns.size, isSearching) {
        if (turns.isNotEmpty() || isSearching) {
            listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
        }
    }

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onDismiss) {
                Text("Close", color = StColors.Accent)
            }
            Text(
                "AI Search",
                color = StColors.Foreground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
            )
            Text(
                "BETA",
                color = StColors.Muted,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color.White.copy(alpha = 0.1f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            TextButton(onClick = onDismiss) {
                Text("Standard", color = StColors.Accent, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (turns.isEmpty()) {
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(StColors.Card)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AutoAwesome, null, tint = StColors.Accent, modifier = Modifier.size(18.dp))
                            Text("Ask like you’d ask a friend", color = StColors.Foreground, fontWeight = FontWeight.SemiBold)
                        }
                        Text(
                            "Describe a mood or situation — I’ll interpret it, explain what I’m looking for, and suggest titles from Story Time.",
                            color = StColors.Muted,
                            fontSize = 13.sp,
                        )
                    }
                }
            }

            items(turns, key = { it.id }) { turn ->
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Text(
                            turn.prompt,
                            color = Color.White,
                            fontSize = 15.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(StColors.Accent.copy(alpha = 0.9f))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                        )
                    }
                    AssistantBlock(
                        turn = turn,
                        picks = filtered(turn.results),
                        onOpenDetail = onOpenDetail,
                        onSuggestion = { send(it) },
                    )
                }
            }

            if (isSearching) {
                item {
                    ThinkingRow()
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
        }

        HorizontalDivider(color = StColors.Border)
        Column(
            Modifier
                .fillMaxWidth()
                .background(StColors.Surface)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (turns.isEmpty()) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    starterPrompts.forEach { prompt ->
                        Text(
                            prompt,
                            color = StColors.Foreground,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable { send(prompt) }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                TextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Describe a vibe…", color = StColors.Muted) },
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
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(query) }),
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = { send(query) },
                    enabled = canSend(),
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (canSend()) StColors.Accent else Color.White.copy(alpha = 0.08f)),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = if (canSend()) Color.Black else StColors.Muted,
                    )
                }
            }
        }
    }
}

@Composable
private fun ThinkingRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StColors.Card)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(Icons.Filled.AutoAwesome, null, tint = StColors.Accent, modifier = Modifier.padding(top = 2.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("Thinking…", color = StColors.Foreground, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Text("Interpreting your vibe and scanning the catalogue.", color = StColors.Muted, fontSize = 12.sp)
            CircularProgressIndicator(color = StColors.Accent, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
private fun AssistantBlock(
    turn: AIChatTurn,
    picks: List<SearchResult>,
    onOpenDetail: (ContentItem) -> Unit,
    onSuggestion: (String) -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(StColors.Card)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.AutoAwesome, null, tint = StColors.Accent, modifier = Modifier.size(14.dp))
            Text("AI", color = StColors.Accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        turn.error?.let {
            Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp)
        }
        turn.report?.let {
            Text(it, color = StColors.Foreground.copy(alpha = 0.92f), fontSize = 13.sp)
        }

        if (turn.moodTags.isNotEmpty()) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                turn.moodTags.forEach { tag ->
                    Text(
                        tag,
                        color = StColors.Muted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.06f))
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                    )
                }
            }
        }

        if (picks.isNotEmpty()) {
            Text("Recommended for you", color = StColors.Foreground, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            picks.forEach { result ->
                ResultRow(result = result, onClick = { onOpenDetail(result.asContentItem()) })
                HorizontalDivider(color = StColors.Border)
            }
        }

        if (turn.suggestions.isNotEmpty()) {
            Text("Ask next", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                turn.suggestions.forEach { suggestion ->
                    Text(
                        suggestion,
                        color = StColors.Accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(StColors.Accent.copy(alpha = 0.12f))
                            .clickable { onSuggestion(suggestion) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        RemoteImage(
            urls = result.posterCandidates,
            modifier = Modifier
                .width(52.dp)
                .height(78.dp)
                .clip(RoundedCornerShape(8.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                result.title,
                color = StColors.Foreground,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val meta = listOfNotNull(result.type, result.category).filter { it.isNotEmpty() }.joinToString(" · ")
            if (meta.isNotEmpty()) {
                Text(meta, color = StColors.Muted, fontSize = 12.sp, maxLines = 1)
            }
        }
        Icon(Icons.Filled.ChevronRight, null, tint = StColors.Muted, modifier = Modifier.size(16.dp))
    }
}

private fun moodChips(prompt: String): List<String> {
    val q = prompt.lowercase()
    val tags = mutableListOf<String>()
    fun add(t: String) { if (t !in tags) tags.add(t) }
    if ("cozy" in q || "rain" in q || "comfort" in q) add("Cozy")
    if ("feel" in q || "wholesome" in q) add("Feel-good")
    if ("late" in q || "dark" in q || "intense" in q) add("Late night")
    if ("funny" in q || "comedy" in q || "laugh" in q) add("Comedy")
    if ("family" in q || "kids" in q) add("Family")
    if ("thriller" in q || "mystery" in q) add("Thriller")
    if ("horror" in q || "scary" in q) add("Horror")
    if ("doc" in q) add("Documentary")
    if ("quick" in q || "short" in q) add("Quick watch")
    if ("romance" in q || "date" in q) add("Romance")
    return tags.take(5)
}
