package com.storytime.universe.ui.player

import android.content.pm.ActivityInfo
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.storytime.universe.findActivity
import com.storytime.universe.data.download.DownloadController
import com.storytime.universe.data.download.EpisodePlaybackInfo
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val playerIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(request: PlaybackRequest?, onClose: () -> Unit) {
    if (request == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val context = LocalContext.current
    val activity = remember { context.findActivity() }

    var episodeId by remember { mutableStateOf(request.episodeId) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var showNextUp by remember { mutableStateOf(false) }

    val queue = request.episodes
    fun currentIndex(): Int = queue.indexOfFirst { it.episodeId == episodeId }
    fun nextEpisode(): EpisodePlaybackInfo? {
        val i = currentIndex()
        return if (i >= 0 && i + 1 < queue.size) queue[i + 1] else null
    }

    val currentTitle = remember(episodeId) {
        queue.firstOrNull { it.episodeId == episodeId }?.let { "${request.title} · ${it.episodeLabel}" }
            ?: if (request.isTrailer) "${request.title} · Trailer" else request.title
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DownloadController.cacheDataSourceFactory()))
            .build()
            .apply { playWhenReady = true }
    }

    // Player state listener
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED && nextEpisode() != null) {
                    showNextUp = true
                }
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                errorMessage = error.localizedMessage ?: "Playback failed"
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Landscape lock + immersive full screen (iOS parity: play locks to landscape)
    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            controller?.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Load / reload when episode changes
    LaunchedEffect(episodeId) {
        isBuffering = true
        errorMessage = null
        showNextUp = false
        try {
            val offline = request.offlineUrl
            val url: String
            val isHls: Boolean
            if (offline != null) {
                url = offline
                isHls = offline.contains(".m3u8")
            } else {
                val bundle = ViewerApi.fetchPlaybackBundle(request.contentId, episodeId, request.isTrailer)
                url = bundle.streamUrl ?: throw IllegalStateException("No playable stream for this title.")
                isHls = bundle.isHls
            }
            val builder = MediaItem.Builder().setUri(url)
            if (isHls) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            exoPlayer.setMediaItem(builder.build())
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true

            val isInitial = episodeId == request.episodeId
            if (isInitial && !request.isTrailer) {
                val (pos, _) = ViewerApi.fetchWatchProgress(request.contentId)
                if (pos > 5) exoPlayer.seekTo(pos * 1000L)
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Playback failed"
        }
    }

    // Periodic progress reporting
    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(10_000)
            if (!request.isTrailer && exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition / 1000.0
                val dur = exoPlayer.duration.takeIf { it > 0 }?.div(1000.0)
                runCatching { ViewerApi.saveWatchProgress(request.contentId, pos, dur) }
            }
        }
    }

    // Final save + release on exit
    DisposableEffect(exoPlayer) {
        onDispose {
            val posMs = exoPlayer.currentPosition
            val durMs = exoPlayer.duration
            val watchedSeconds = (posMs / 1000.0).coerceAtLeast(0.0)
            val contentId = request.contentId
            val trailer = request.isTrailer
            exoPlayer.release()
            if (!trailer && watchedSeconds > 0) {
                playerIoScope.launch {
                    runCatching {
                        ViewerApi.saveWatchProgress(contentId, watchedSeconds, durMs.takeIf { it > 0 }?.div(1000.0))
                        ViewerApi.recordWatchSession(contentId, watchedSeconds)
                    }
                }
            }
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Top overlay: back + title
        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.size(42.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).clickable { onClose() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = Color.White)
            }
            Text(currentTitle, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        if (isBuffering && errorMessage == null) {
            CircularProgressIndicator(color = StColors.Accent, modifier = Modifier.align(Alignment.Center))
        }

        errorMessage?.let { msg ->
            Column(
                Modifier.align(Alignment.Center).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Can't play this title", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(msg, color = StColors.Muted, fontSize = 13.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Retry",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(CircleShape).background(Color.White).clickable {
                            errorMessage = null
                            episodeId = episodeId // retrigger not guaranteed; force via prepare
                            exoPlayer.prepare()
                        }.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                    Text(
                        "Close",
                        color = Color.White,
                        modifier = Modifier.clip(CircleShape).background(Color.White.copy(alpha = 0.16f)).clickable { onClose() }.padding(horizontal = 20.dp, vertical = 10.dp),
                    )
                }
            }
        }

        // Next-up card
        val next = nextEpisode()
        if (showNextUp && next != null) {
            Column(
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .width(280.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFF1A1A1A))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("Up Next", color = StColors.Muted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Text("${next.episodeLabel} · ${next.title}", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(
                    Modifier.clip(CircleShape).background(Color.White).clickable { episodeId = next.episodeId }.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Text("Play next", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}
