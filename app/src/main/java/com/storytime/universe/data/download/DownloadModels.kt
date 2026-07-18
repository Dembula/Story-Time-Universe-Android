package com.storytime.universe.data.download

import kotlinx.serialization.Serializable

/** UI-facing lifecycle state, mirroring the iOS `DownloadState`. */
enum class UiDownloadState { QUEUED, DOWNLOADING, COMPLETED, FAILED, PAUSED }

/** Everything needed to begin a download without re-fetching content metadata (iOS `DownloadSpec`). */
data class DownloadSpec(
    val contentId: String,
    val episodeId: String? = null,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val type: String? = null,
    val durationSeconds: Int? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
) {
    val key: String get() = makeKey(contentId, episodeId)

    fun toMeta(): DownloadMeta = DownloadMeta(
        contentId = contentId,
        episodeId = episodeId,
        title = title,
        subtitle = subtitle,
        posterUrl = posterUrl,
        type = type,
        durationSeconds = durationSeconds,
        seasonNumber = seasonNumber,
        episodeNumber = episodeNumber,
    )

    companion object {
        fun makeKey(contentId: String, episodeId: String?): String =
            if (!episodeId.isNullOrEmpty()) "$contentId|$episodeId" else contentId
    }
}

/** Persisted alongside a Media3 DownloadRequest (`request.data`). */
@Serializable
data class DownloadMeta(
    val contentId: String,
    val episodeId: String? = null,
    val title: String,
    val subtitle: String? = null,
    val posterUrl: String? = null,
    val type: String? = null,
    val durationSeconds: Int? = null,
    val seasonNumber: Int? = null,
    val episodeNumber: Int? = null,
)

/** A download surfaced to the UI: parsed metadata + live progress/state. */
data class DownloadEntry(
    val key: String,
    val uri: String,
    val meta: DownloadMeta,
    val state: UiDownloadState,
    val progress: Float,
    val createdAtMs: Long,
) {
    val isPlayableOffline: Boolean get() = state == UiDownloadState.COMPLETED
    val contentId: String get() = meta.contentId
    val episodeId: String? get() = meta.episodeId
}

/** A single item in an in-player "up next" queue (series episodes). iOS `EpisodePlaybackInfo`. */
data class EpisodePlaybackInfo(
    val episodeId: String,
    val title: String,
    val episodeLabel: String,
    val thumbnailUrl: String?,
    val durationSeconds: Int?,
)
