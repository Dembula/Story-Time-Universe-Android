package com.storytime.universe.data.model

/**
 * Mirrors Story Time Production `src/lib/content-types.ts`.
 * Home rows fill as creators upload — no app update needed for known types.
 */
object CatalogueTypes {

    data class RowDefinition(
        val id: String,
        val typeValues: List<String>,
        val categoryFilter: String? = null,
        val title: String,
        val reserveEmptySlot: Boolean = false,
    )

    val labels: Map<String, String> = mapOf(
        "MOVIE" to "Movie",
        "DOCUMENTARY" to "Documentary",
        "SHORT_FILM" to "Short Film",
        "SERIES" to "Series",
        "SHOW" to "Show",
        "PODCAST" to "Podcast",
        "COMEDY_SKIT" to "Comedy Skit",
        "STAND_UP" to "Stand-Up",
        "ANIMATION" to "Animation",
        "SPORTS" to "Sports",
        "MUSIC_VIDEO" to "Music Video",
        "LIVE_EVENT" to "Live Event",
        "REALITY" to "Reality",
        "NEWS" to "News",
        "EDUCATIONAL" to "Educational",
        "WEB_SERIES" to "Web Series",
    )

    val pluralLabels: Map<String, String> = mapOf(
        "MOVIE" to "Movies",
        "SERIES" to "Series",
        "SHOW" to "Shows",
        "DOCUMENTARY" to "Documentaries",
        "SHORT_FILM" to "Short Films",
        "PODCAST" to "Podcasts",
        "COMEDY_SKIT" to "Comedy Skits",
        "STAND_UP" to "Stand-Up",
        "ANIMATION" to "Animation",
        "SPORTS" to "Sports",
        "MUSIC_VIDEO" to "Music Videos",
        "LIVE_EVENT" to "Live Events",
        "REALITY" to "Reality",
        "WEB_SERIES" to "Web Series",
        "NEWS" to "News",
        "EDUCATIONAL" to "Educational",
    )

    val longFormTypes: Set<String> = setOf(
        "SERIES", "SHOW", "PODCAST", "WEB_SERIES", "REALITY", "NEWS",
    )

    val allCatalogueTypeValues: List<String> = listOf(
        "MOVIE", "SERIES", "SHOW", "DOCUMENTARY", "SHORT_FILM", "PODCAST",
        "COMEDY_SKIT", "STAND_UP", "ANIMATION", "SPORTS", "MUSIC_VIDEO",
        "LIVE_EVENT", "REALITY", "WEB_SERIES", "NEWS", "EDUCATIONAL",
    )

    val primaryHomeRows: List<RowDefinition> = listOf(
        RowDefinition("MOVIE", listOf("MOVIE"), title = "Movies"),
        RowDefinition("SERIES", listOf("SERIES"), title = "Series"),
        RowDefinition("ANIMATION", listOf("ANIMATION"), title = "Animation"),
        RowDefinition("SPORTS", listOf("SPORTS"), title = "Sports"),
        RowDefinition("COMEDY", listOf("COMEDY_SKIT", "STAND_UP"), title = "Comedy"),
        RowDefinition("DOCUMENTARY", listOf("DOCUMENTARY"), title = "Documentaries"),
        RowDefinition("SHOW", listOf("SHOW"), title = "Shows"),
        RowDefinition("PODCAST", listOf("PODCAST"), title = "Podcasts"),
    )

    val secondaryHomeRows: List<RowDefinition> = listOf(
        RowDefinition("LIVE_EVENT", listOf("LIVE_EVENT"), title = "Live Events"),
        RowDefinition("COMEDY_SHOWS", listOf("SHOW"), categoryFilter = "Comedy", title = "Comedy Shows"),
        RowDefinition("SHORT_FILM", listOf("SHORT_FILM"), title = "Short Films"),
        RowDefinition("MUSIC_VIDEO", listOf("MUSIC_VIDEO"), title = "Music Videos"),
        RowDefinition("REALITY", listOf("REALITY"), title = "Reality"),
        RowDefinition("WEB_SERIES", listOf("WEB_SERIES"), title = "Web Series"),
        RowDefinition("NEWS", listOf("NEWS"), title = "News"),
        RowDefinition("EDUCATIONAL", listOf("EDUCATIONAL"), title = "Educational"),
    )

    val allHomeRows: List<RowDefinition> get() = primaryHomeRows + secondaryHomeRows

    val allTrackedTypeValues: Set<String>
        get() = allHomeRows.flatMap { it.typeValues }.toSet() + allCatalogueTypeValues

    fun pluralTitle(typeValue: String): String {
        pluralLabels[typeValue]?.let { return it }
        return labels[typeValue] ?: typeValue.replace("_", " ")
            .lowercase()
            .split(" ")
            .joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    fun isLongForm(type: String?): Boolean {
        if (type == null) return false
        return longFormTypes.contains(type.uppercase())
    }
}

data class HomeCatalogRow(
    val id: String,
    val typeValue: String,
    val title: String,
    val items: List<ContentItem>,
    val reserveEmptySlot: Boolean,
) {
    val shouldDisplay: Boolean get() = items.isNotEmpty()
}
