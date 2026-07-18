package com.storytime.universe.ui.detail

import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.download.DownloadSpec
import com.storytime.universe.data.download.EpisodePlaybackInfo
import com.storytime.universe.data.media.MediaUrl
import com.storytime.universe.data.model.BtsVideo
import com.storytime.universe.data.model.ContentDetail
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.CrewCredit
import com.storytime.universe.data.model.Episode
import com.storytime.universe.data.model.PersonRoute
import com.storytime.universe.data.model.Season
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.components.DownloadButton
import com.storytime.universe.ui.components.DownloadButtonStyle
import com.storytime.universe.ui.components.RemoteImage
import com.storytime.universe.ui.home.PosterCard
import com.storytime.universe.ui.main.NavActions
import com.storytime.universe.ui.player.PlaybackRequest
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.launch

@Composable
fun ContentDetailScreen(contentId: String, seed: ContentItem?, actions: NavActions, onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    var detail by remember { mutableStateOf<ContentDetail?>(null) }
    var crew by remember { mutableStateOf<List<CrewCredit>>(emptyList()) }
    var related by remember { mutableStateOf<List<ContentItem>>(emptyList()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var inWatchlist by remember { mutableStateOf(false) }
    var watchlistBusy by remember { mutableStateOf(false) }

    LaunchedEffect(contentId) {
        try {
            val loaded = ViewerApi.fetchContentDetail(contentId)
            detail = loaded
            crew = runCatching { ViewerApi.fetchCrew(contentId) }.getOrDefault(emptyList())
            related = runCatching {
                ViewerApi.fetchRelated(contentId, loaded.category, loaded.type, 12)
            }.getOrDefault(emptyList())
            val list = runCatching { ViewerApi.fetchWatchlist() }.getOrNull()
            inWatchlist = list?.any { it.id == contentId } ?: false
        } catch (e: Exception) {
            errorMessage = e.localizedMessage
        }
    }

    val title = detail?.title ?: seed?.title ?: ""
    val heroUrls = detail?.backdropCandidates?.takeIf { it.isNotEmpty() }
        ?: seed?.backdropCandidates?.takeIf { it.isNotEmpty() }
        ?: detail?.posterCandidates?.takeIf { it.isNotEmpty() }
        ?: seed?.posterCandidates ?: emptyList()

    val episodeInfos = remember(detail) { buildEpisodeInfos(detail) }
    val firstEpisodeId = detail?.seasons?.firstOrNull()?.episodes?.firstOrNull()?.id

    fun play(trailer: Boolean, episodeId: String?) {
        actions.play(
            PlaybackRequest(
                contentId = contentId,
                title = title,
                episodeId = episodeId,
                isTrailer = trailer,
                episodes = if (trailer) emptyList() else episodeInfos,
            )
        )
    }

    Box(Modifier.fillMaxSize().background(StColors.Background)) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            // Hero
            Box(Modifier.fillMaxWidth().height(420.dp)) {
                RemoteImage(urls = heroUrls, modifier = Modifier.fillMaxSize())
                Box(
                    Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f), Color.Black.copy(alpha = 0.95f))
                        )
                    )
                )
                Column(
                    Modifier.align(Alignment.BottomStart).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(title, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold, maxLines = 3, overflow = TextOverflow.Ellipsis)
                    val meta = buildMeta(detail, seed)
                    if (meta.isNotEmpty()) {
                        Text(meta, color = StColors.Muted, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                    detail?.ratingStats?.let { rating ->
                        if ((rating.count ?: 0) > 0) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.Star, null, tint = StColors.Accent, modifier = Modifier.size(16.dp))
                                Text(String.format("%.1f", rating.average ?: 0.0), color = Color.White, fontWeight = FontWeight.SemiBold)
                                Text("(${rating.count})", color = StColors.Muted)
                            }
                        }
                    }
                    // Action row
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.weight(1f).clip(CircleShape).background(Color.White).clickable { play(false, firstEpisodeId) }.padding(vertical = 14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Filled.PlayArrow, null, tint = Color.Black)
                                Text("Play", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        }
                        if (detail?.hasTrailer == true) {
                            CircleIcon(Icons.Filled.Movie, "Trailer") { play(true, null) }
                        }
                        CircleIcon(if (inWatchlist) Icons.Filled.Check else Icons.Filled.Add, "My List") {
                            if (!watchlistBusy) {
                                watchlistBusy = true
                                scope.launch {
                                    try {
                                        ViewerApi.updateWatchlist(contentId, !inWatchlist)
                                        inWatchlist = !inWatchlist
                                    } catch (e: Exception) {
                                        errorMessage = e.localizedMessage
                                    } finally {
                                        watchlistBusy = false
                                    }
                                }
                            }
                        }
                    }
                    // Film download (series episodes are downloaded individually)
                    if (detail?.seasons.isNullOrEmpty()) {
                        DownloadButton(
                            spec = DownloadSpec(
                                contentId = contentId,
                                title = title,
                                posterUrl = detail?.posterUrl ?: seed?.posterUrl,
                                type = detail?.type ?: seed?.type,
                                durationSeconds = detail?.duration,
                            ),
                            style = DownloadButtonStyle.LABELED,
                        )
                    }
                }
            }

            // Body
            Column(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(28.dp),
            ) {
                val synopsis = (detail?.description ?: seed?.description)?.trim()?.takeIf { it.isNotEmpty() }
                if (synopsis != null) SynopsisSection(synopsis)

                if (detail?.hasTrailer == true) {
                    TrailersSection(detail?.backdropCandidates ?: detail?.posterCandidates ?: emptyList()) { play(true, null) }
                }

                val seasons = detail?.seasons ?: emptyList()
                if (seasons.isNotEmpty()) {
                    EpisodesSection(seasons, title, contentId, detail?.type) { play(false, it) }
                }

                if (crew.isNotEmpty()) CastSection(crew) { actions.openPerson(it) }

                if (related.isNotEmpty()) RelatedSection(related) { actions.openDetail(it) }

                detail?.btsVideos?.takeIf { it.isNotEmpty() }?.let { BtsSection(it) }

                errorMessage?.let { Text(it, color = Color(0xFFFF5A5A), fontSize = 13.sp) }
                Spacer(Modifier.height(20.dp))
            }
        }

        // Back button
        Box(
            Modifier
                .padding(16.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable { onBack() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White)
        }
    }
}

@Composable
private fun CircleIcon(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = Color.White)
    }
}

@Composable
private fun SynopsisSection(text: String) {
    var expanded by remember { mutableStateOf(false) }
    val limit = 220
    val needsExpansion = text.length > limit
    val shown = if (!needsExpansion || expanded) text else {
        val end = text.take(limit)
        val snippet = end.substringBeforeLast(' ', end)
        snippet.trim() + "…"
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.animateContentSize()) {
        Text(shown, color = StColors.Muted, fontSize = 15.sp, lineHeight = 21.sp)
        if (needsExpansion) {
            Text(
                if (expanded) "See less" else "See more",
                color = StColors.Accent,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { expanded = !expanded },
            )
        }
    }
}

@Composable
private fun TrailersSection(imageUrls: List<String>, onPlay: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Trailers")
        Box(
            Modifier
                .width(280.dp)
                .height(158.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable { onPlay() },
        ) {
            RemoteImage(urls = imageUrls, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))))
            Icon(Icons.Filled.PlayCircle, null, tint = Color.White, modifier = Modifier.align(Alignment.Center).size(44.dp))
            Text("Official Trailer", color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp))
        }
    }
}

@Composable
private fun EpisodesSection(seasons: List<Season>, seriesTitle: String, seriesContentId: String, contentType: String?, onPlayEpisode: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SectionTitle("Episodes")
        seasons.forEach { season ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Season ${season.seasonNumber ?: 1}", color = StColors.AccentGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                (season.episodes ?: emptyList()).forEach { episode ->
                    EpisodeRow(
                        episode = episode,
                        spec = DownloadSpec(
                            contentId = seriesContentId,
                            episodeId = episode.id,
                            title = seriesTitle,
                            subtitle = "S${season.seasonNumber ?: 1} E${episode.episodeNumber ?: 0} · ${episode.title ?: "Episode"}",
                            posterUrl = episode.thumbnailUrl,
                            type = contentType,
                            durationSeconds = episode.duration,
                            seasonNumber = season.seasonNumber,
                            episodeNumber = episode.episodeNumber,
                        ),
                        onPlay = { onPlayEpisode(episode.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(episode: Episode, spec: DownloadSpec, onPlay: () -> Unit) {
    val thumb = episode.thumbnailUrl?.let { MediaUrl.resolve(posterUrl = it, videoUrl = episode.videoUrl) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.weight(1f).clickable { onPlay() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.width(120.dp).height(68.dp).clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
                if (thumb != null) RemoteImage(url = thumb, modifier = Modifier.fillMaxSize())
                else Icon(Icons.Filled.PlayArrow, null, tint = Color.White.copy(alpha = 0.8f))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("${episode.episodeNumber ?: 0}. ${episode.title ?: "Episode"}", color = StColors.Foreground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                episode.description?.let { Text(it, color = StColors.Muted, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis) }
                if ((episode.duration ?: 0) > 0) Text("${episode.duration} min", color = StColors.Muted, fontSize = 11.sp)
            }
        }
        DownloadButton(spec = spec, style = DownloadButtonStyle.ICON)
    }
}

@Composable
private fun CastSection(crew: List<CrewCredit>, onSelect: (PersonRoute) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Cast & Crew")
            Spacer(Modifier.weight(1f))
            Text("${crew.size} credited", color = StColors.Muted, fontSize = 12.sp)
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            items(crew, key = { it.id }) { member ->
                Column(
                    Modifier.width(88.dp).clickable { onSelect(PersonRoute.from(member)) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        Modifier.size(72.dp).clip(CircleShape).background(
                            Brush.linearGradient(listOf(StColors.Accent.copy(alpha = 0.7f), StColors.profileColor(member.id)))
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(member.initials, color = Color.White, fontWeight = FontWeight.Bold)
                    }
                    Text(member.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text(member.role ?: "Crew", color = StColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun RelatedSection(items: List<ContentItem>, onSelect: (ContentItem) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("More Like This")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(items, key = { it.id }) { item ->
                Column(Modifier.width(118.dp).clickable { onSelect(item) }, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PosterCard(item = item)
                    Text(item.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text((item.type ?: "").replace("_", " "), color = StColors.Muted, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun BtsSection(videos: List<BtsVideo>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle("Behind the Scenes")
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(videos, key = { it.id }) { video ->
                Column(Modifier.width(200.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    RemoteImage(urls = video.thumbnailCandidates, modifier = Modifier.width(200.dp).height(112.dp).clip(RoundedCornerShape(12.dp)))
                    Text(video.title ?: "Behind the Scenes", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = StColors.Foreground, fontSize = 20.sp, fontWeight = FontWeight.Bold)
}

private fun buildMeta(detail: ContentDetail?, seed: ContentItem?): String {
    val parts = mutableListOf<String>()
    (detail?.type ?: seed?.type)?.let { parts.add(it.replace("_", " ").replaceFirstChar { c -> c.uppercase() }) }
    (detail?.category ?: seed?.category)?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
    (detail?.year ?: seed?.year)?.let { parts.add(it.toString()) }
    detail?.runtimeLabel?.let { parts.add(it) }
    detail?.ageRating?.takeIf { it.isNotEmpty() }?.let { parts.add(it) }
    detail?.creator?.name?.takeIf { it.isNotEmpty() }?.let { parts.add("By $it") }
    return parts.joinToString(" · ")
}

private fun buildEpisodeInfos(detail: ContentDetail?): List<EpisodePlaybackInfo> {
    val seasons = detail?.seasons ?: return emptyList()
    val list = mutableListOf<EpisodePlaybackInfo>()
    for (season in seasons) {
        val sNum = season.seasonNumber ?: 1
        for (episode in season.episodes ?: emptyList()) {
            val eNum = episode.episodeNumber ?: (list.size + 1)
            list.add(
                EpisodePlaybackInfo(
                    episodeId = episode.id,
                    title = episode.title ?: "Episode $eNum",
                    episodeLabel = "S$sNum E$eNum",
                    thumbnailUrl = episode.thumbnailUrl,
                    durationSeconds = episode.duration,
                )
            )
        }
    }
    return list
}
