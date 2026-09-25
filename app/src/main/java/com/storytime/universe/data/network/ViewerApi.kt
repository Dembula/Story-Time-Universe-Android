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
import com.storytime.universe.data.model.PpvCheckoutResponse
import com.storytime.universe.data.model.TitleAccessResult
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

    /**
     * Select account package model (Subscription plan or PPV).
     * PPV activates immediately with no checkout; subscription may require payment.
     */
    suspend fun selectViewerPackage(
        viewerModel: String,
        plan: String,
        startTrial: Boolean = false,
    ): ViewerSubscription? {
        val body = mapOf(
            "viewerModel" to viewerModel,
            "plan" to plan,
            "startTrial" to startTrial,
        )
        val result = api.request(path = "api/viewer/subscription", method = "POST", jsonBody = body)
        if (!result.isSuccess) throw api.parseApiError(result)
        // Response may wrap subscription or return it directly — prefer nested then refresh.
        val nested = runCatching { api.decode<SubscriptionResponse>(result).subscription }.getOrNull()
        if (nested != null) return nested
        return fetchSubscription()
    }

    suspend fun requestPpvAccess(contentId: String): PpvCheckoutResponse {
        val result = api.request(
            path = "api/viewer/ppv",
            method = "POST",
            jsonBody = mapOf("contentId" to contentId),
        )
        if (!result.isSuccess) throw api.parseApiError(result)
        return api.decode(result)
    }

    /** Gate Play for PPV accounts before opening the player (iOS parity). */
    suspend fun resolveTitleAccess(
        contentId: String,
        isPayPerViewAccount: Boolean,
        isTrailer: Boolean,
    ): TitleAccessResult {
        if (isTrailer) return TitleAccessResult.Playable
        if (!isPayPerViewAccount) return TitleAccessResult.Playable
        return try {
            val result = requestPpvAccess(contentId)
            when {
                result.alreadyOwned == true -> TitleAccessResult.Playable
                result.requiresPayment == true ||
                    !result.checkoutUrl.isNullOrEmpty() ||
                    result.success == false -> TitleAccessResult.RequiresPurchase(contentId)
                else -> TitleAccessResult.Playable
            }
        } catch (e: ApiException.PaymentRequired) {
            TitleAccessResult.RequiresPurchase(contentId)
        } catch (e: Exception) {
            TitleAccessResult.Blocked(e.localizedMessage ?: "Could not verify title access.")
        }
    }

    /**
     * Attach a verified Google Play subscription purchase to the signed-in viewer.
     * Tries known production endpoints (same pattern as iOS Apple activate).
     */
    suspend fun activateGoogleSubscription(
        productId: String,
        purchaseToken: String,
        orderId: String?,
        packageName: String,
        planCode: String,
    ) {
        val body = mapOf(
            "productId" to productId,
            "purchaseToken" to purchaseToken,
            "orderId" to orderId,
            "packageName" to packageName,
            "plan" to planCode,
            "planCode" to planCode,
            "platform" to "android",
            "source" to "android_app",
        )
        postGoogleActivate(
            candidates = listOf(
                "api/viewer/google/activate",
                "api/viewer/google/subscription",
                "api/viewer/play/activate",
                "api/viewer/play/subscription",
                "api/viewer/iap/subscription",
                "api/billing/google/activate",
                "api/payments/google/activate",
            ),
            body = body,
        )
    }

    /** Attach a verified Google Play PPV unlock to a content id. */
    suspend fun activateGooglePpv(
        contentId: String,
        productId: String,
        purchaseToken: String,
        orderId: String?,
        packageName: String,
    ) {
        val body = mapOf(
            "contentId" to contentId,
            "productId" to productId,
            "purchaseToken" to purchaseToken,
            "orderId" to orderId,
            "packageName" to packageName,
            "plan" to "PPV_FILM",
            "planCode" to "PPV_FILM",
            "platform" to "android",
            "source" to "android_app",
            "kind" to "ppv",
            "accessDays" to 7,
        )
        postGoogleActivate(
            candidates = listOf(
                "api/viewer/google/ppv",
                "api/viewer/play/ppv",
                "api/viewer/iap/ppv",
                "api/billing/google/ppv",
                "api/payments/google/ppv",
                "api/viewer/google/activate",
            ),
            body = body,
            notFoundMessage = "Purchase completed in Google Play, but the server cannot unlock titles from Android yet. " +
                "Open the title on story-time.online to finish PayFast unlock, or contact support with your Play order ID.",
        )
    }

    private suspend fun postGoogleActivate(
        candidates: List<String>,
        body: Map<String, Any?>,
        notFoundMessage: String = "Purchase completed in Google Play, but the server cannot activate plans from Android yet. " +
            "Contact support with your Play order ID, or open Manage subscription on the website.",
    ) {
        var lastError: Exception = ApiException.Server(notFoundMessage)
        var sawNotFound = true
        for (path in candidates) {
            val result = try {
                api.request(path = path, method = "POST", jsonBody = body)
            } catch (e: Exception) {
                lastError = e
                continue
            }
            if (result.isSuccess) return
            if (result.code == 404) continue
            sawNotFound = false
            lastError = api.parseApiError(result)
            if (result.code in listOf(401, 402, 403)) throw lastError
        }
        if (sawNotFound) throw ApiException.Server(notFoundMessage)
        throw lastError
    }
}
