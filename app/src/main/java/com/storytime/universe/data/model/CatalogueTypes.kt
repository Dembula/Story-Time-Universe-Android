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

    val seedGenres: List<String> = listOf(
        "Action", "Adventure", "Animation", "Biography", "Comedy", "Crime", "Documentary",
        "Drama", "Family", "Fantasy", "Feel-Good", "History", "Horror", "Music", "Mystery",
        "Romance", "Sci-Fi", "Sport", "Thriller", "War", "Western",
    )

    private val genreAliases: Map<String, String> = mapOf(
        "sci fi" to "Sci-Fi", "scifi" to "Sci-Fi", "science fiction" to "Sci-Fi",
        "feel good" to "Feel-Good", "feelgood" to "Feel-Good",
        "doc" to "Documentary", "docs" to "Documentary",
        "kids" to "Family", "children" to "Family",
        "sports" to "Sport",
    )

    fun canonicalGenre(from: String?): String? {
        var value = from?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        while (value.startsWith("#")) value = value.drop(1).trim()
        if (value.isEmpty()) return null
        val key = value.lowercase()
            .replace("_", " ")
            .replace("-", " ")
            .replace("&", "and")
            .replace(Regex("\\s+"), " ")
            .trim()
        genreAliases[key]?.let { return it }
        genreAliases[value.lowercase()]?.let { return it }
        for (genre in seedGenres) {
            val g = genre.lowercase()
            if (key == g || key.startsWith("$g ") || key.endsWith(" $g") || key.contains(" $g ")) {
                return genre
            }
        }
        return null
    }

    fun populatedGenres(from: List<ContentItem>): List<String> {
        val found = linkedSetOf<String>()
        for (item in from) {
            canonicalGenre(item.category)?.let { found.add(it) }
            item.tags?.split(',', ';', '|')?.forEach { part ->
                canonicalGenre(part)?.let { found.add(it) }
            }
        }
        return found.sortedWith(String.CASE_INSENSITIVE_ORDER)
    }

    val browseTypeOptions: List<BrowseTypeOption> = listOf(
        BrowseTypeOption("ALL", "Home", emptyList()),
        BrowseTypeOption("MOVIE", "Movies", listOf("MOVIE")),
        BrowseTypeOption("SERIES", "Series", listOf("SERIES")),
        BrowseTypeOption("SHOW", "Shows", listOf("SHOW")),
        BrowseTypeOption("DOCUMENTARY", "Documentaries", listOf("DOCUMENTARY")),
        BrowseTypeOption("ANIMATION", "Animation", listOf("ANIMATION")),
        BrowseTypeOption("SPORTS", "Sports", listOf("SPORTS")),
        BrowseTypeOption("COMEDY", "Comedy", listOf("COMEDY_SKIT", "STAND_UP")),
        BrowseTypeOption("SHORT_FILM", "Short Films", listOf("SHORT_FILM")),
        BrowseTypeOption("PODCAST", "Podcasts", listOf("PODCAST")),
        BrowseTypeOption("MUSIC_VIDEO", "Music Videos", listOf("MUSIC_VIDEO")),
        BrowseTypeOption("LIVE_EVENT", "Live Events", listOf("LIVE_EVENT")),
        BrowseTypeOption("REALITY", "Reality", listOf("REALITY")),
        BrowseTypeOption("WEB_SERIES", "Web Series", listOf("WEB_SERIES")),
    )
}

data class BrowseTypeOption(val id: String, val title: String, val typeValues: List<String>)

data class HomeCatalogRow(
    val id: String,
    val typeValue: String,
    val title: String,
    val items: List<ContentItem>,
    val reserveEmptySlot: Boolean,
) {
    val shouldDisplay: Boolean get() = items.isNotEmpty()
}

sealed class HomeBrowseFilter {
    data object All : HomeBrowseFilter()
    data class ContentType(val id: String, val title: String, val typeValues: List<String>) : HomeBrowseFilter()
    data class Genre(val name: String) : HomeBrowseFilter()

    val chromeTitle: String
        get() = when (this) {
            All -> "Home"
            is ContentType -> title
            is Genre -> name
        }
}

data class CatalogueListRequest(
    val id: String,
    val title: String,
    val typeValues: List<String> = emptyList(),
    val categoryFilter: String? = null,
    val genre: String? = null,
    val seedItems: List<ContentItem> = emptyList(),
    val continueWatching: List<ContinueWatchingItem> = emptyList(),
)

fun ContentItem.matchesGenre(genre: String): Boolean {
    val target = CatalogueTypes.canonicalGenre(from = genre)
        ?: genre.trim().takeIf { it.isNotEmpty() }
        ?: return false
    val targetKey = target.lowercase()
    CatalogueTypes.canonicalGenre(from = category)?.let {
        if (it.lowercase() == targetKey) return true
    }
    if (category?.lowercase()?.contains(targetKey) == true) return true
    tags?.lowercase()?.let { if (it.contains(targetKey)) return true }
    return false
}
