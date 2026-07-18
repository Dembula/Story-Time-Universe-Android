package com.storytime.universe.data.network

import com.storytime.universe.data.model.CatalogueTypes
import com.storytime.universe.data.model.ContentDetail
import com.storytime.universe.data.model.ContentItem
import com.storytime.universe.data.model.ContinueWatchingItem
import com.storytime.universe.data.model.CrewCredit
import com.storytime.universe.data.model.PersonPreview
import com.storytime.universe.data.model.PersonRoute
import com.storytime.universe.data.model.PlaybackBundle
import com.storytime.universe.data.model.SearchResponse
import com.storytime.universe.data.model.SearchResult
import com.storytime.universe.data.model.SubscriptionResponse
import com.storytime.universe.data.model.ViewerProfile
import com.storytime.universe.data.model.ViewerSubscription
import com.storytime.universe.data.model.ActiveProfileResponse
import com.storytime.universe.data.model.ProfilesResponse
import com.storytime.universe.data.model.WatchProgress
import com.storytime.universe.data.model.WatchlistContentRow
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.jsonArray

/** Ported from the iOS `ViewerAPI` actor. Every endpoint & behavior mirrors the iOS client. */
object ViewerApi {

    private val api = ApiClient

    // MARK: Profiles

    suspend fun fetchProfiles(): List<ViewerProfile> {
        val result = api.request(path = "api/viewer/profiles")
        if (result.code != 200) throw api.parseApiError(result)
        return api.decode<ProfilesResponse>(result).profiles
    }

    suspend fun activateProfile(id: String, pin: String? = null): ViewerProfile {
        val body = HashMap<String, Any?>()
        body["profileId"] = id
        if (!pin.isNullOrEmpty()) body["pin"] = pin

        val result = api.request(
            path = "api/viewer/profiles/active",
            method = "POST",
            jsonBody = body,
        )
        if (result.code == 402) throw api.parseApiError(result)
        if (result.code == 401 || result.code == 403) {
            val err = runCatching { api.decode<ActiveProfileResponse>(result) }.getOrNull()
            if (err?.requiresPin == true) {
                throw ApiException.Server(err.error ?: "PIN required")
            }
            throw api.parseApiError(result)
        }
        if (!result.isSuccess) throw api.parseApiError(result)

        val decoded = api.decode<ActiveProfileResponse>(result)
        val profile = decoded.profile ?: throw ApiException.Server(decoded.error ?: "Failed to select profile")
        api.setViewerProfileCookie(profile.id)
        return profile
    }

    suspend fun createProfile(
        name: String,
        birthYear: Int,
        birthMonth: Int,
        birthDay: Int,
        pin: String?,
    ): ViewerProfile {
        val body = HashMap<String, Any?>()
        body["name"] = name
        body["birthYear"] = birthYear
        body["birthMonth"] = birthMonth
        body["birthDay"] = birthDay
        if (pin != null && pin.length == 4) {
            body["pinEnabled"] = true
            body["pin"] = pin
        }
        val result = api.request(path = "api/viewer/profiles", method = "POST", jsonBody = body)
        if (!result.isSuccess) throw api.parseApiError(result)
        return api.decode<ActiveProfileResponse>(result).profile
            ?: throw ApiException.Server("Failed to create profile")
    }

    // MARK: Catalogue

    suspend fun fetchContent(
        type: String? = null,
        featured: Boolean = false,
        category: String? = null,
        limit: Int = 20,
    ): List<ContentItem> {
        val query = ArrayList<Pair<String, String>>()
        query.add("limit" to limit.toString())
        if (type != null) query.add("type" to type)
        if (featured) query.add("featured" to "true")
        if (category != null) query.add("category" to category)
        val result = api.request(path = "api/content", query = query)
        if (result.code != 200) throw api.parseApiError(result)
        return decodeContentList(result)
    }

    /** Fetch one Home row that may span multiple type values (and optional category). */
    suspend fun fetchCatalogRow(definition: CatalogueTypes.RowDefinition, limit: Int = 16): List<ContentItem> {
        if (definition.typeValues.size == 1 && definition.categoryFilter == null) {
            return runCatching { fetchContent(type = definition.typeValues[0], limit = limit) }.getOrDefault(emptyList())
        }

        val combined = ArrayList<ContentItem>()
        val seen = HashSet<String>()

        for (typeValue in definition.typeValues) {
            val batch = runCatching {
                fetchContent(type = typeValue, category = definition.categoryFilter, limit = limit)
            }.getOrDefault(emptyList())
            for (item in batch) {
                if (seen.add(item.id)) combined.add(item)
            }
            if (combined.size >= limit) break
        }

        if (combined.isEmpty() && definition.categoryFilter != null) {
            val byCategory = runCatching {
                fetchContent(category = definition.categoryFilter, limit = limit)
            }.getOrDefault(emptyList())
            for (item in byCategory) {
                val type = item.type?.uppercase() ?: ""
                if (definition.typeValues.isEmpty() || definition.typeValues.contains(type)) {
                    if (seen.add(item.id)) combined.add(item)
                }
            }
        }

        return combined.take(limit)
    }

    private fun decodeContentList(result: HttpResult): List<ContentItem> {
        runCatching { return api.decode<List<ContentItem>>(result) }
        // Decode row-by-row so one bad item can't blank the UI.
        val array = runCatching { ApiClient.json.parseToJsonElement(result.bodyString()).jsonArray }.getOrNull()
            ?: return emptyList()
        return array.mapNotNull { element ->
            runCatching { ApiClient.json.decodeFromJsonElement(ContentItem.serializer(), element) }.getOrNull()
        }
    }

    suspend fun fetchContinueWatching(): List<ContinueWatchingItem> {
        val result = api.request(path = "api/watch/continue-watching")
        if (result.code != 200) return emptyList()
        return runCatching {
            api.json.decodeFromString(ListSerializer(ContinueWatchingItem.serializer()), result.bodyString())
        }.getOrDefault(emptyList())
    }

    suspend fun fetchContentDetail(id: String): ContentDetail {
        val result = api.request(path = "api/content/$id")
        if (result.code != 200) throw api.parseApiError(result)
        return api.decode<ContentDetail>(result)
    }

    suspend fun fetchCrew(contentId: String): List<CrewCredit> {
        val result = api.request(path = "api/crew", query = listOf("contentId" to contentId))
        if (result.code != 200) return emptyList()
        return runCatching {
            api.json.decodeFromString(ListSerializer(CrewCredit.serializer()), result.bodyString())
        }.getOrDefault(emptyList())
    }

    suspend fun fetchPersonPreview(personId: String): PersonPreview {
        val result = api.request(path = "api/people/$personId/preview")
        if (result.code != 200) throw api.parseApiError(result)
        return api.decode<PersonPreview>(result)
    }

    suspend fun fetchPersonPreviewByCrew(crewMemberId: String): PersonPreview {
        val result = api.request(path = "api/people/preview", query = listOf("crewMemberId" to crewMemberId))
        if (result.code != 200) throw api.parseApiError(result)
        return api.decode<PersonPreview>(result)
    }

    suspend fun fetchPerson(route: PersonRoute): PersonPreview {
        val personId = route.personId
        if (!personId.isNullOrEmpty()) {
            try {
                return fetchPersonPreview(personId)
            } catch (e: Exception) {
                val crewMemberId = route.crewMemberId
                if (!crewMemberId.isNullOrEmpty()) return fetchPersonPreviewByCrew(crewMemberId)
                throw e
            }
        }
        val crewMemberId = route.crewMemberId
        if (!crewMemberId.isNullOrEmpty()) return fetchPersonPreviewByCrew(crewMemberId)
        throw ApiException.Server("No person profile is linked to this credit.")
    }

    suspend fun fetchRelated(excluding: String, category: String?, type: String?, limit: Int = 12): List<ContentItem> {
        var items: List<ContentItem> = emptyList()
        if (!category.isNullOrEmpty()) {
            items = fetchContent(category = category, limit = limit + 4)
        }
        if (items.size < 4 && type != null) {
            items = items + fetchContent(type = type, limit = limit + 4)
        }
        if (items.isEmpty()) {
            items = fetchContent(limit = limit + 4)
        }
        val seen = HashSet<String>()
        return items.filter { item ->
            if (item.id == excluding) false else seen.add(item.id)
        }.take(limit)
    }

    suspend fun fetchPlaybackBundle(contentId: String, episodeId: String? = null, trailer: Boolean = false): PlaybackBundle {
        val query = ArrayList<Pair<String, String>>()
        if (episodeId != null) query.add("episodeId" to episodeId)
        if (trailer) query.add("trailer" to "1")
        val result = api.request(path = "api/content/$contentId/playback-bundle", query = query)
        if (result.code != 200) throw api.parseApiError(result)
        return api.decode<PlaybackBundle>(result)
    }

    suspend fun fetchWatchProgress(contentId: String): Pair<Int, Int?> {
        val result = api.request(path = "api/watch/progress", query = listOf("contentId" to contentId))
        if (result.code != 200) return 0 to null
        val progress = runCatching { api.decode<WatchProgress>(result) }.getOrNull()
        return (progress?.positionSeconds ?: 0) to progress?.durationSeconds
    }

    suspend fun saveWatchProgress(contentId: String, positionSeconds: Double, durationSeconds: Double?) {
        val body = HashMap<String, Any?>()
        body["contentId"] = contentId
        body["positionSeconds"] = positionSeconds
        if (durationSeconds != null) body["durationSeconds"] = durationSeconds
        runCatching { api.request(path = "api/watch/progress", method = "PUT", jsonBody = body) }
    }

    suspend fun recordWatchSession(contentId: String, durationSeconds: Double) {
        runCatching {
            api.request(
                path = "api/watch",
                method = "POST",
                jsonBody = mapOf("contentId" to contentId, "durationSeconds" to durationSeconds),
            )
        }
    }

    suspend fun search(query: String): List<SearchResult> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val result = api.request(
            path = "api/browse/search",
            query = listOf("q" to q, "limit" to "24"),
        )
        if (result.code != 200) return emptyList()
        return api.decode<SearchResponse>(result).results
    }

    suspend fun fetchWatchlist(): List<ContentItem> {
        val result = api.request(path = "api/watchlist")
        if (result.code != 200) throw api.parseApiError(result)
        val rows = runCatching {
            api.json.decodeFromString(ListSerializer(WatchlistContentRow.serializer()), result.bodyString())
        }.getOrDefault(emptyList())
        return rows.mapNotNull { it.content }
    }

    suspend fun updateWatchlist(contentId: String, add: Boolean) {
        val result = api.request(
            path = "api/watchlist",
            method = "POST",
            jsonBody = mapOf("contentId" to contentId, "action" to if (add) "add" else "remove"),
        )
        if (!result.isSuccess) throw api.parseApiError(result)
    }

    suspend fun fetchSubscription(): ViewerSubscription? {
        val result = api.request(path = "api/viewer/subscription")
        if (result.code != 200) return null
        return api.decode<SubscriptionResponse>(result).subscription
    }
}
