package com.storytime.universe.ui.player

import com.storytime.universe.data.download.EpisodePlaybackInfo

/** A request to open the full-screen player, mirroring the iOS `PlaybackRequest`. */
data class PlaybackRequest(
    val contentId: String,
    val title: String,
    val episodeId: String? = null,
    val isTrailer: Boolean = false,
    val episodes: List<EpisodePlaybackInfo> = emptyList(),
    /** When set, play this cached/local stream directly instead of fetching a playback bundle. */
    val offlineUrl: String? = null,
)
