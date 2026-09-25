package com.storytime.universe.data.model

import com.storytime.universe.data.AppConfig
import com.storytime.universe.data.media.MediaUrl
import kotlinx.serialization.Serializable

@Serializable
data class SessionUser(
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val image: String? = null,
    val role: String? = null,
)

@Serializable
data class AuthSession(
    val user: SessionUser? = null,
    val expires: String? = null,
)

@Serializable
data class ViewerProfile(
    val id: String,
    val name: String,
    val age: Int = 0,
    val dateOfBirth: String? = null,
    val updatedAt: String? = null,
    val pinEnabled: Boolean? = null,
) {
    val isKids: Boolean get() = age <= 12

    val ageLabel: String
        get() = when {
            age <= 12 -> "Kids"
            age <= 15 -> "Teen"
            else -> "Adult"
        }
}

@Serializable
data class ProfilesResponse(val profiles: List<ViewerProfile> = emptyList())

@Serializable
data class ActiveProfileResponse(
    val profile: ViewerProfile? = null,
    val ok: Boolean? = null,
    val error: String? = null,
    val requiresPin: Boolean? = null,
    val paymentRequired: Boolean? = null,
)

@Serializable
data class ContentItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: String? = null,
    val category: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val year: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val trailerUrl: String? = null,
    val videoUrl: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val duration: Int? = null,
    val featured: Boolean? = null,
    @Serializable(with = FlexibleStringSerializer::class) val tags: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val minAge: Int? = null,
) {
    val displayType: String
        get() = (type ?: "TITLE").replace("_", " ")
            .lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }

    val posterCandidates: List<String>
        get() = MediaUrl.candidates(posterUrl, backdropUrl, videoUrl, preferBackdrop = false)

    val backdropCandidates: List<String>
        get() = MediaUrl.candidates(posterUrl, backdropUrl, videoUrl, preferBackdrop = true)
            .ifEmpty { posterCandidates }
}

@Serializable
data class ContinueWatchingItem(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: String? = null,
    val category: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val videoUrl: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val duration: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val positionSeconds: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val durationSeconds: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val progressPercent: Int? = null,
) {
    val posterCandidates: List<String>
        get() = MediaUrl.candidates(posterUrl, backdropUrl, videoUrl, preferBackdrop = false)

    val backdropCandidates: List<String>
        get() = MediaUrl.candidates(posterUrl, backdropUrl, videoUrl, preferBackdrop = true)
            .ifEmpty { posterCandidates }

    val progress: Double
        get() {
            progressPercent?.let { return (it / 100.0).coerceIn(0.0, 1.0) }
            val pos = (positionSeconds ?: 0).toDouble()
            val dur = (durationSeconds ?: duration ?: 0).toDouble()
            if (dur <= 0) return 0.0
            return (pos / dur).coerceIn(0.0, 1.0)
        }

    fun asContentItem(): ContentItem = ContentItem(
        id = id,
        title = title,
        description = description,
        type = type,
        category = category,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        videoUrl = videoUrl,
        duration = durationSeconds ?: duration,
    )
}

@Serializable
data class CreatorInfo(
    val id: String? = null,
    val name: String? = null,
    val image: String? = null,
)

@Serializable
data class RatingStats(
    val average: Double? = null,
    @Serializable(with = FlexibleIntSerializer::class) val count: Int? = null,
)

@Serializable
data class Episode(
    val id: String,
    val title: String? = null,
    val description: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val episodeNumber: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val duration: Int? = null,
    val thumbnailUrl: String? = null,
    val videoUrl: String? = null,
)

@Serializable
data class Season(
    val id: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val seasonNumber: Int? = null,
    val title: String? = null,
    val episodes: List<Episode>? = null,
) {
    val stableId: String get() = id ?: "season-${seasonNumber ?: 0}"
}

@Serializable
data class BtsVideo(
    val id: String,
    val title: String? = null,
    val videoUrl: String? = null,
    val thumbnail: String? = null,
) {
    val thumbnailCandidates: List<String>
        get() = MediaUrl.candidates(thumbnail, null, videoUrl, preferBackdrop = false)
}

@Serializable
data class CrewCredit(
    val id: String,
    val name: String,
    val role: String? = null,
    val bio: String? = null,
    val creditPersonId: String? = null,
) {
    val initials: String
        get() {
            val parts = name.split(" ").filter { it.isNotEmpty() }.take(2)
            val chars = parts.mapNotNull { it.firstOrNull() }
            return if (chars.isEmpty()) name.take(1).uppercase() else chars.joinToString("").uppercase()
        }
}

@Serializable
data class ContentDetail(
    val id: String,
    val title: String,
    val description: String? = null,
    val type: String? = null,
    val category: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val year: Int? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val trailerUrl: String? = null,
    val videoUrl: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val duration: Int? = null,
    @Serializable(with = FlexibleStringSerializer::class) val tags: String? = null,
    val language: String? = null,
    val country: String? = null,
    val ageRating: String? = null,
    val creator: CreatorInfo? = null,
    val ratingStats: RatingStats? = null,
    val seasons: List<Season>? = null,
    val btsVideos: List<BtsVideo>? = null,
) {
    val posterCandidates: List<String>
        get() = MediaUrl.candidates(posterUrl, backdropUrl, videoUrl, preferBackdrop = false)

    val backdropCandidates: List<String>
        get() = MediaUrl.candidates(posterUrl, backdropUrl, videoUrl, preferBackdrop = true)
            .ifEmpty { posterCandidates }

    val hasTrailer: Boolean
        get() = !trailerUrl?.trim().isNullOrEmpty()

    val runtimeLabel: String?
        get() {
            val d = duration ?: return null
            if (d <= 0) return null
            val hours = d / 60
            val mins = d % 60
            return if (hours > 0) "${hours}h ${mins}m" else "$mins min"
        }

    fun asContentItem(): ContentItem = ContentItem(
        id = id,
        title = title,
        description = description,
        type = type,
        category = category,
        year = year,
        posterUrl = posterUrl,
        backdropUrl = backdropUrl,
        trailerUrl = trailerUrl,
        videoUrl = videoUrl,
        duration = duration,
        tags = tags,
    )
}

@Serializable
data class PlaybackSource(
    val src: String? = null,
    val type: String? = null,
)

@Serializable
data class SubtitleTrack(
    val id: String,
    val language: String? = null,
    val label: String? = null,
    val vttUrl: String? = null,
    val isDefault: Boolean? = null,
)

@Serializable
data class PlaybackBundle(
    val id: String? = null,
    val title: String? = null,
    val playback: PlaybackSource? = null,
    val posterUrl: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val duration: Int? = null,
    val subtitles: List<SubtitleTrack>? = null,
) {
    val streamUrl: String?
        get() {
            val src = playback?.src
            if (src.isNullOrEmpty()) return null
            if (src.startsWith("http")) return src
            return AppConfig.API_BASE_URL.trimEnd('/') + "/" + src.trimStart('/')
        }

    val isHls: Boolean
        get() {
            val t = playback?.type?.lowercase().orEmpty()
            return t.contains("mpegurl") || (streamUrl?.contains(".m3u8") == true)
        }
}

@Serializable
data class WatchlistContentRow(val content: ContentItem? = null)

@Serializable
data class SearchResponse(val results: List<SearchResult> = emptyList())

@Serializable
data class SearchResult(
    val id: String,
    val title: String,
    val type: String? = null,
    val category: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val year: Int? = null,
    val posterUrl: String? = null,
    val creatorName: String? = null,
) {
    val posterCandidates: List<String>
        get() = MediaUrl.candidates(posterUrl, null, null, preferBackdrop = false)

    fun asContentItem(): ContentItem = ContentItem(
        id = id,
        title = title,
        type = type,
        category = category,
        year = year,
        posterUrl = posterUrl,
    )
}

@Serializable
data class ViewerSubscription(
    val id: String? = null,
    val plan: String? = null,
    val status: String? = null,
    val viewerModel: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val profileLimit: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val deviceCount: Int? = null,
    val currentPeriodEnd: String? = null,
    val cancelAtPeriodEnd: Boolean? = null,
) {
    /** True when account was set up as pay-per-title (not unlimited subscription). */
    val isPayPerViewModel: Boolean
        get() {
            val model = viewerModel?.trim()?.uppercase().orEmpty()
            val p = plan?.trim()?.uppercase().orEmpty()
            return looksLikePayPerView(model) || looksLikePayPerView(p)
        }

    companion object {
        private fun looksLikePayPerView(value: String): Boolean {
            if (value.isEmpty()) return false
            if (value == "PPV" || value == "PPV_FILM" || value == "PAY_PER_VIEW") return true
            if (value.contains("PPV")) return true
            if (value.contains("PAY_PER_VIEW") || value.contains("PAY-PER-VIEW") || value.contains("PAY PER VIEW")) return true
            return false
        }
    }
}

/** Result of `POST /api/viewer/ppv`. */
@Serializable
data class PpvCheckoutResponse(
    val success: Boolean? = null,
    val requiresPayment: Boolean? = null,
    val alreadyOwned: Boolean? = null,
    val checkoutUrl: String? = null,
    val error: String? = null,
)

/** Gate Play for PPV accounts before opening the player. */
sealed class TitleAccessResult {
    data object Playable : TitleAccessResult()
    data class RequiresPurchase(val contentId: String) : TitleAccessResult()
    data class Blocked(val message: String) : TitleAccessResult()
}

@Serializable
data class SubscriptionResponse(val subscription: ViewerSubscription? = null)

@Serializable
data class ApiErrorBody(
    val error: String? = null,
    val requiresPin: Boolean? = null,
    val paymentRequired: Boolean? = null,
)

@Serializable
data class WatchProgress(
    @Serializable(with = FlexibleIntSerializer::class) val positionSeconds: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val durationSeconds: Int? = null,
)

@Serializable
data class CsrfResponse(val csrfToken: String)

// MARK: - Person / credits (matches web PersonPreview)

@Serializable
data class PersonLatestProject(
    val id: String,
    val title: String,
    val type: String? = null,
    val posterUrl: String? = null,
)

@Serializable
data class PersonCredit(
    val contentId: String,
    val title: String,
    val type: String? = null,
    val role: String,
    val posterUrl: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val year: Int? = null,
) {
    val posterCandidates: List<String>
        get() = MediaUrl.candidates(posterUrl, null, null, preferBackdrop = false)

    fun asContentItem(): ContentItem = ContentItem(
        id = contentId,
        title = title,
        type = type,
        year = year,
        posterUrl = posterUrl,
    )
}

@Serializable
data class PersonPreview(
    val personId: String,
    val displayName: String,
    val imageUrl: String? = null,
    val roles: List<String>? = null,
    val bio: String? = null,
    val blurb: String? = null,
    @Serializable(with = FlexibleIntSerializer::class) val productionCount: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val followerCount: Int? = null,
    @Serializable(with = FlexibleIntSerializer::class) val followingCount: Int? = null,
    val verified: Boolean? = null,
    val profileHref: String? = null,
    val latestProject: PersonLatestProject? = null,
    val topGenres: List<String>? = null,
    val isCreator: Boolean? = null,
    val creatorUserId: String? = null,
    val credits: List<PersonCredit>? = null,
) {
    val imageCandidates: List<String>
        get() = MediaUrl.candidates(imageUrl, null, null, preferBackdrop = false)

    val initials: String
        get() {
            val parts = displayName.split(" ").filter { it.isNotEmpty() }.take(2)
            val chars = parts.mapNotNull { it.firstOrNull() }
            return if (chars.isEmpty()) displayName.take(1).uppercase() else chars.joinToString("").uppercase()
        }
}

/** Navigation payload when tapping a cast/crew credit (not serialized). */
data class PersonRoute(
    val personId: String? = null,
    val crewMemberId: String? = null,
    val fallbackName: String,
    val fallbackRole: String? = null,
    val fallbackBio: String? = null,
) {
    val id: String get() = personId ?: crewMemberId ?: fallbackName

    companion object {
        fun from(member: CrewCredit): PersonRoute = PersonRoute(
            personId = member.creditPersonId,
            crewMemberId = member.id,
            fallbackName = member.name,
            fallbackRole = member.role,
            fallbackBio = member.bio,
        )
    }
}
