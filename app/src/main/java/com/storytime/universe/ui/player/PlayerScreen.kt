package com.storytime.universe.ui.player

import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.net.Uri
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import androidx.annotation.OptIn
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.ClosedCaptionDisabled
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PictureInPictureAlt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import com.storytime.universe.data.download.DownloadController
import com.storytime.universe.data.download.EpisodePlaybackInfo
import com.storytime.universe.data.model.SubtitleTrack
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.findActivity
import com.storytime.universe.ui.components.RemoteImage
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val playerIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

private const val SEEK_STEP_MS = 10_000L
private const val CONTROLS_AUTO_HIDE_MS = 3_200L

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(request: PlaybackRequest?, onClose: () -> Unit) {
    if (request == null) {
        LaunchedEffect(Unit) { onClose() }
        return
    }
    val context = LocalContext.current
    val activity = remember { context.findActivity() }
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { maxOf(1, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)) }

    var episodeId by remember { mutableStateOf(request.episodeId) }
    var reloadToken by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isBuffering by remember { mutableStateOf(true) }
    var showNextUp by remember { mutableStateOf(false) }
    var controlsVisible by remember { mutableStateOf(true) }
    var showEpisodes by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var isLocked by remember { mutableStateOf(false) }
    var captionsEnabled by remember { mutableStateOf(true) }
    var hasCaptions by remember { mutableStateOf(false) }

    var feedback by remember { mutableStateOf<String?>(null) }
    var feedbackTick by remember { mutableIntStateOf(0) }

    var brightness by remember {
        mutableFloatStateOf(activity?.window?.attributes?.screenBrightness?.takeIf { it in 0f..1f } ?: 0.5f)
    }
    var volumeFraction by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / maxVolume)
    }

    val playerViewRef = remember { mutableStateOf<PlayerView?>(null) }
    val episodeListState = rememberLazyListState()
    val inPip = PipController.isInPip
    val pipSupported = remember(activity) { activity != null && PipController.isSupported(activity) }

    DisposableEffect(Unit) {
        PipController.markPlayerActive(true)
        activity?.let { PipController.syncParams(it) }
        onDispose {
            // Keep PiP alive if the system mini-player is already showing.
            if (!PipController.isInPip) {
                PipController.markPlayerActive(false)
            }
        }
    }

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
            .setSeekBackIncrementMs(SEEK_STEP_MS)
            .setSeekForwardIncrementMs(SEEK_STEP_MS)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DownloadController.cacheDataSourceFactory()))
            .build()
            .apply {
                playWhenReady = true
                trackSelectionParameters = trackSelectionParameters
                    .buildUpon()
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                    .setSelectUndeterminedTextLanguage(true)
                    .build()
            }
    }

    fun applyCaptionsEnabled(enabled: Boolean) {
        captionsEnabled = enabled
        exoPlayer.trackSelectionParameters = exoPlayer.trackSelectionParameters
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !enabled)
            .build()
        playerViewRef.value?.subtitleView?.visibility =
            if (enabled) View.VISIBLE else View.GONE
    }

    fun flashFeedback(text: String) {
        feedback = text
        feedbackTick++
    }

    fun setChromeVisible(visible: Boolean) {
        controlsVisible = visible
        val pv = playerViewRef.value ?: return
        if (isLocked) {
            pv.hideController()
            return
        }
        if (visible) pv.showController() else pv.hideController()
    }

    fun toggleControls() {
        val next = !controlsVisible
        setChromeVisible(next)
        if (!next) showEpisodes = false
    }

    fun adjustBrightness(deltaFraction: Float) {
        if (isLocked) return
        val window = activity?.window ?: return
        val newValue = (brightness + deltaFraction).coerceIn(0.02f, 1f)
        if (abs(newValue - brightness) < 0.004f) return
        brightness = newValue
        val lp = window.attributes
        lp.screenBrightness = newValue
        window.attributes = lp
        flashFeedback("Brightness  ${(newValue * 100).roundToInt()}%")
    }

    fun adjustVolume(deltaFraction: Float) {
        if (isLocked) return
        val newValue = (volumeFraction + deltaFraction).coerceIn(0f, 1f)
        if (abs(newValue - volumeFraction) < 0.004f) return
        volumeFraction = newValue
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (newValue * maxVolume).roundToInt(), 0)
        flashFeedback("Volume  ${(newValue * 100).roundToInt()}%")
    }

    fun doubleTapAt(x: Float) {
        if (isLocked) return
        val pv = playerViewRef.value ?: return
        val third = pv.width / 3f
        when {
            x < third -> {
                exoPlayer.seekBack()
                flashFeedback("- 10s")
            }
            x > third * 2 -> {
                exoPlayer.seekForward()
                flashFeedback("+ 10s")
            }
            else -> if (exoPlayer.isPlaying) exoPlayer.pause() else exoPlayer.play()
        }
        setChromeVisible(true)
    }

    fun restartPlayback() {
        if (isLocked) return
        exoPlayer.seekTo(0)
        exoPlayer.playWhenReady = true
        flashFeedback("Restart")
        setChromeVisible(true)
    }

    fun lockScreen() {
        isLocked = true
        showEpisodes = false
        setChromeVisible(true)
        flashFeedback("Screen locked")
    }

    fun unlockScreen() {
        isLocked = false
        setChromeVisible(true)
        flashFeedback("Unlocked")
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                isBuffering = state == Player.STATE_BUFFERING
                if (state == Player.STATE_ENDED && nextEpisode() != null && !isLocked) {
                    showNextUp = true
                    setChromeVisible(true)
                }
            }

            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                if (!playing && !isLocked) setChromeVisible(true)
            }

            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                errorMessage = error.localizedMessage ?: "Playback failed"
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    PipController.updateAspectRatio(videoSize.width, videoSize.height)
                    activity?.let { PipController.syncParams(it) }
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    DisposableEffect(activity) {
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        val window = activity?.window
        val controller = window?.let { WindowInsetsControllerCompat(it, it.decorView) }
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.let {
                val lp = it.attributes
                lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = lp
            }
        }
    }

    LaunchedEffect(episodeId, reloadToken) {
        isBuffering = true
        errorMessage = null
        showNextUp = false
        hasCaptions = false
        try {
            val offline = request.offlineUrl
            val url: String
            val isHls: Boolean
            val subs: List<SubtitleTrack>
            if (offline != null) {
                url = offline
                isHls = offline.contains(".m3u8")
                subs = emptyList()
            } else {
                val bundle = ViewerApi.fetchPlaybackBundle(request.contentId, episodeId, request.isTrailer)
                url = bundle.streamUrl ?: throw IllegalStateException("No playable stream for this title.")
                isHls = bundle.isHls
                subs = bundle.subtitles.orEmpty()
            }
            val builder = MediaItem.Builder().setUri(url)
            if (isHls) builder.setMimeType(MimeTypes.APPLICATION_M3U8)
            val subtitleConfigs = subs.mapNotNull { track ->
                val vtt = track.absoluteVttUrl ?: return@mapNotNull null
                MediaItem.SubtitleConfiguration.Builder(Uri.parse(vtt))
                    .setMimeType(MimeTypes.TEXT_VTT)
                    .setLanguage(track.language)
                    .setLabel(track.label ?: track.language ?: "Subtitles")
                    .setSelectionFlags(
                        if (track.isDefault == true) C.SELECTION_FLAG_DEFAULT else 0
                    )
                    .build()
            }
            hasCaptions = subtitleConfigs.isNotEmpty()
            if (subtitleConfigs.isNotEmpty()) {
                builder.setSubtitleConfigurations(subtitleConfigs)
            }
            exoPlayer.setMediaItem(builder.build())
            exoPlayer.prepare()
            exoPlayer.playWhenReady = true
            applyCaptionsEnabled(captionsEnabled)
            setChromeVisible(true)

            val isInitial = episodeId == request.episodeId && reloadToken == 0
            if (isInitial && !request.isTrailer && offline == null) {
                val (pos, _) = runCatching { ViewerApi.fetchWatchProgress(request.contentId) }.getOrDefault(0 to null)
                if (pos > 5) exoPlayer.seekTo(pos * 1000L)
            }
        } catch (e: Exception) {
            errorMessage = e.localizedMessage ?: "Playback failed"
        }
    }

    LaunchedEffect(feedbackTick) {
        if (feedback != null) {
            delay(750)
            feedback = null
        }
    }

    // Auto-hide chrome while playing (restart + title + transport fade together).
    LaunchedEffect(controlsVisible, isPlaying, showEpisodes, showNextUp, errorMessage, isLocked) {
        if (!controlsVisible || showEpisodes || showNextUp || errorMessage != null) return@LaunchedEffect
        if (!isPlaying && !isLocked) return@LaunchedEffect
        delay(CONTROLS_AUTO_HIDE_MS)
        if ((isPlaying || isLocked) && !showEpisodes && !showNextUp && errorMessage == null) {
            setChromeVisible(false)
        }
    }

    LaunchedEffect(episodeId, queue) {
        val idx = currentIndex()
        if (idx >= 0) {
            runCatching { episodeListState.animateScrollToItem(idx) }
        }
    }

    var lastReportedWatchSeconds by remember(exoPlayer) { mutableStateOf(0.0) }

    LaunchedEffect(exoPlayer) {
        while (true) {
            delay(10_000)
            if (!request.isTrailer && request.offlineUrl == null && exoPlayer.isPlaying) {
                val pos = exoPlayer.currentPosition / 1000.0
                val dur = exoPlayer.duration.takeIf { it > 0 }?.div(1000.0)
                runCatching { ViewerApi.saveWatchProgress(request.contentId, pos, dur) }
                val delta = (pos - lastReportedWatchSeconds).coerceAtLeast(0.0)
                if (delta >= 30.0) {
                    lastReportedWatchSeconds = pos
                    runCatching { ViewerApi.recordWatchSession(request.contentId, delta) }
                }
            }
        }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            val posMs = exoPlayer.currentPosition
            val durMs = exoPlayer.duration
            val watchedSeconds = (posMs / 1000.0).coerceAtLeast(0.0)
            val contentId = request.contentId
            val trailer = request.isTrailer
            val offline = request.offlineUrl != null
            val previouslyReported = lastReportedWatchSeconds
            exoPlayer.release()
            if (!trailer && !offline && watchedSeconds > 0) {
                val remaining = (watchedSeconds - previouslyReported).coerceAtLeast(0.0)
                playerIoScope.launch {
                    runCatching {
                        ViewerApi.saveWatchProgress(contentId, watchedSeconds, durMs.takeIf { it > 0 }?.div(1000.0))
                        if (remaining >= 5.0) {
                            ViewerApi.recordWatchSession(contentId, remaining)
                        }
                    }
                }
            }
        }
    }

    val chromeAlpha by animateFloatAsState(
        if (controlsVisible && !inPip) 1f else 0f,
        label = "chromeAlpha",
    )

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                var gestureAxis: Int = 0 // 0 undecided, 1 vertical, 2 horizontal
                val gestureDetector = GestureDetector(ctx, object : GestureDetector.SimpleOnGestureListener() {
                    override fun onDown(e: MotionEvent): Boolean {
                        gestureAxis = 0
                        return true
                    }

                    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                        if (PipController.isInPip) return true
                        toggleControls()
                        return true
                    }

                    override fun onDoubleTap(e: MotionEvent): Boolean {
                        if (PipController.isInPip) return true
                        doubleTapAt(e.x)
                        return true
                    }

                    override fun onScroll(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        distanceX: Float,
                        distanceY: Float,
                    ): Boolean {
                        if (PipController.isInPip) return false
                        val pv = playerViewRef.value ?: return false
                        if (e1 == null || pv.height == 0 || pv.width == 0) return false
                        if (gestureAxis == 0) {
                            gestureAxis = if (abs(distanceY) > abs(distanceX) * 1.15f) 1 else 2
                        }
                        if (gestureAxis != 1) return false
                        val delta = (distanceY / pv.height.toFloat()) * 1.35f
                        if (e1.x < pv.width / 2f) adjustBrightness(delta) else adjustVolume(delta)
                        return true
                    }
                })

                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                    setControllerShowTimeoutMs(CONTROLS_AUTO_HIDE_MS.toInt())
                    setShowNextButton(false)
                    setShowPreviousButton(false)
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setBackgroundColor(android.graphics.Color.BLACK)
                    subtitleView?.apply {
                        setApplyEmbeddedFontSizes(false)
                        setBottomPaddingFraction(0.12f)
                        setStyle(
                            CaptionStyleCompat(
                                android.graphics.Color.WHITE,
                                android.graphics.Color.TRANSPARENT,
                                android.graphics.Color.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW,
                                android.graphics.Color.BLACK,
                                null,
                            )
                        )
                    }
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            if (!isLocked) {
                                controlsVisible = visibility == View.VISIBLE
                            }
                        }
                    )
                    @Suppress("ClickableViewAccessibility")
                    setOnTouchListener { _, event ->
                        gestureDetector.onTouchEvent(event)
                        true
                    }
                    playerViewRef.value = this
                }
            },
            update = { view ->
                view.useController = !inPip && !isLocked
                if (inPip || isLocked) view.hideController()
                view.subtitleView?.visibility =
                    if (captionsEnabled && hasCaptions) View.VISIBLE else View.GONE
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (!inPip && isLocked) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(horizontal = 16.dp, vertical = 14.dp)
                    .alpha(chromeAlpha),
            ) {
                CircleControl(enabled = controlsVisible, onClick = { unlockScreen() }) {
                    Icon(Icons.Filled.Lock, "Unlock", tint = Color.White)
                }
            }
        }

        if (!inPip && !isLocked) {
        // Top chrome: back + title + captions + lock + restart + PiP
        Row(
            Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .alpha(chromeAlpha),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircleControl(enabled = controlsVisible, onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Close", tint = Color.White)
            }
            Text(
                currentTitle,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (hasCaptions && !request.isTrailer) {
                CircleControl(enabled = controlsVisible, onClick = {
                    val next = !captionsEnabled
                    applyCaptionsEnabled(next)
                    flashFeedback(if (next) "Captions on" else "Captions off")
                    setChromeVisible(true)
                }) {
                    Icon(
                        if (captionsEnabled) Icons.Filled.ClosedCaption else Icons.Filled.ClosedCaptionDisabled,
                        "Captions",
                        tint = if (captionsEnabled) StColors.Accent else Color.White,
                    )
                }
            }
            if (!request.isTrailer) {
                CircleControl(enabled = controlsVisible, onClick = { lockScreen() }) {
                    Icon(Icons.Filled.LockOpen, "Lock screen", tint = Color.White)
                }
            }
            if (pipSupported && !request.isTrailer) {
                CircleControl(enabled = controlsVisible, onClick = {
                    activity?.let { PipController.enter(it) }
                }) {
                    Icon(Icons.Filled.PictureInPictureAlt, "Mini player", tint = Color.White)
                }
            }
            CircleControl(enabled = controlsVisible, onClick = { restartPlayback() }) {
                Icon(Icons.Filled.Replay, "Restart", tint = Color.White)
            }
            if (queue.isNotEmpty()) {
                Text(
                    "Episodes",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.16f))
                        .clickable(enabled = controlsVisible) {
                            showEpisodes = !showEpisodes
                            setChromeVisible(true)
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                )
            }
        }
        }

        if (!inPip) {
        feedback?.let { text ->
            Text(
                text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.Black.copy(alpha = 0.6f))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }

        if (isBuffering && errorMessage == null && feedback == null) {
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
                            reloadToken++
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

        // Bottom episode strip (series / web series / seasons)
        if (!isLocked && queue.isNotEmpty() && (showEpisodes || controlsVisible) && errorMessage == null) {
            Column(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .alpha(if (showEpisodes) 1f else chromeAlpha)
                    .background(Color.Black.copy(alpha = 0.72f))
                    .padding(top = 10.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${queue.size} episodes",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                    )
                    Text(
                        "  · tap to switch",
                        color = StColors.Muted,
                        fontSize = 12.sp,
                    )
                }
                LazyRow(
                    state = episodeListState,
                    contentPadding = PaddingValues(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(queue, key = { it.episodeId }) { ep ->
                        val selected = ep.episodeId == episodeId
                        Column(
                            Modifier
                                .width(148.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .border(
                                    width = if (selected) 2.dp else 0.dp,
                                    color = if (selected) StColors.Accent else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp),
                                )
                                .background(Color.White.copy(alpha = 0.08f))
                                .clickable {
                                    episodeId = ep.episodeId
                                    showEpisodes = false
                                    setChromeVisible(true)
                                }
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(74.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                val thumbs = listOfNotNull(ep.thumbnailUrl)
                                if (thumbs.isNotEmpty()) {
                                    RemoteImage(urls = thumbs, modifier = Modifier.fillMaxSize())
                                } else {
                                    Icon(Icons.Filled.PlayArrow, null, tint = Color.White.copy(alpha = 0.7f))
                                }
                            }
                            Text(ep.episodeLabel, color = StColors.Accent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Text(ep.title, color = Color.White, fontSize = 12.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }

        val next = nextEpisode()
        if (showNextUp && next != null && !isLocked) {
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
                Text(
                    "${next.episodeLabel} · ${next.title}",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    Modifier.clip(CircleShape).background(Color.White).clickable {
                        episodeId = next.episodeId
                    }.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(18.dp))
                    Text("Play next", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
        } // end if (!inPip)
    }
}

@Composable
private fun CircleControl(enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
