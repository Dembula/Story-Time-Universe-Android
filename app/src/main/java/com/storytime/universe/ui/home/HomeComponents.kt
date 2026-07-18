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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.ContinueWatchingItem
import com.storytime.universe.ui.components.RemoteImage
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.delay

@Composable
fun PosterCard(item: ContentItem, rank: Int? = null, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(118.dp)
            .height(176.dp)
            .clip(RoundedCornerShape(12.dp)),
    ) {
        RemoteImage(urls = item.posterCandidates, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
        )
        Column(Modifier.align(Alignment.BottomStart).padding(8.dp)) {
            Text(item.title, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (!item.category.isNullOrEmpty()) {
                Text(item.category, color = StColors.Muted, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (rank != null) {
            Text(
                rank.toString(),
                color = Color.White.copy(alpha = 0.92f),
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.TopStart).padding(start = 2.dp),
            )
        }
    }
}

@Composable
fun ContentRow(title: String, items: List<ContentItem>, onSelect: (ContentItem) -> Unit) {
    if (items.isEmpty()) return
    val isTrending = title.lowercase().contains("trending")
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, color = StColors.Foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 20.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { idx, item ->
                PosterCard(
                    item = item,
                    rank = if (isTrending) idx + 1 else null,
                    modifier = Modifier.clickable { onSelect(item) },
                )
            }
        }
    }
}

@Composable
fun ContinueWatchingRow(items: List<ContinueWatchingItem>, onSelect: (ContinueWatchingItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Continue Watching", color = StColors.Foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = StColors.Muted, modifier = Modifier.size(18.dp))
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp),
        ) {
            itemsIndexed(items, key = { _, it -> it.id }) { _, item ->
                Column(
                    Modifier.width(168.dp).clickable { onSelect(item) },
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier
                            .width(168.dp)
                            .height(96.dp)
                            .clip(RoundedCornerShape(10.dp)),
                    ) {
                        RemoteImage(urls = item.backdropCandidates, modifier = Modifier.fillMaxSize())
                        LinearProgressIndicator(
                            progress = { item.progress.toFloat() },
                            color = StColors.Accent,
                            trackColor = StColors.ProgressTrack,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                    Text(item.title, color = StColors.Foreground, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun HeroCarousel(items: List<ContentItem>, onPlay: (ContentItem) -> Unit, onOpen: (ContentItem) -> Unit) {
    if (items.isEmpty()) return
    val pagerState = rememberPagerState(pageCount = { items.size })

    LaunchedEffect(items.size) {
        if (items.size <= 1) return@LaunchedEffect
        while (true) {
            delay(5000)
            if (items.size <= 1) break
            val next = (pagerState.currentPage + 1) % items.size
            pagerState.animateScrollToPage(next)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        HorizontalPager(
            state = pagerState,
            pageSpacing = 12.dp,
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp),
        ) { page ->
            HeroCard(items[page], onPlay = { onPlay(items[page]) }, onOpen = { onOpen(items[page]) })
        }
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
        ) {
            items.indices.forEach { i ->
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (i == pagerState.currentPage) 18.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (i == pagerState.currentPage) StColors.Accent else Color.White.copy(alpha = 0.28f))
                )
            }
        }
    }
}

@Composable
private fun HeroCard(item: ContentItem, onPlay: () -> Unit, onOpen: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(440.dp)
            .clip(RoundedCornerShape(18.dp)),
    ) {
        RemoteImage(urls = item.backdropCandidates, modifier = Modifier.fillMaxSize())
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.92f))
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "Featured",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            )
            Text(item.title, color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(
                listOfNotNull(item.displayType, item.category).filter { it.isNotEmpty() }.joinToString(" • "),
                color = StColors.Muted,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .weight(1f)
                        .clip(CircleShape)
                        .background(StColors.PlayButton)
                        .clickable { onPlay() }
                        .padding(vertical = 13.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(Icons.Filled.PlayArrow, null, tint = StColors.PlayButtonForeground)
                        Text("Play", color = StColors.PlayButtonForeground, fontWeight = FontWeight.Bold)
                    }
                }
                Box(
                    Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable { onOpen() },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Filled.Add, "More info", tint = Color.White)
                }
            }
        }
    }
}
