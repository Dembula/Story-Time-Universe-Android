package com.storytime.universe.ui

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.storytime.universe.data.NetworkMonitor
import com.storytime.universe.data.SessionStore
import com.storytime.universe.data.billing.BillingService
import com.storytime.universe.data.billing.PaywallContext
import com.storytime.universe.data.download.DownloadController
import com.storytime.universe.data.network.ApiClient
import com.storytime.universe.data.network.AuthService
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.data.model.AuthSession
import com.storytime.universe.data.model.TitleAccessResult
import com.storytime.universe.data.model.ViewerProfile
import com.storytime.universe.data.model.ViewerSubscription
import com.storytime.universe.ui.player.PlaybackRequest
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/** Top-level router + session state, ported from the iOS `AppState`. */
class AppState(application: Application) : AndroidViewModel(application) {

    enum class Route { LOADING, SIGN_IN, PROFILES, MAIN }

    var route by mutableStateOf(Route.LOADING)
        private set
    var session by mutableStateOf<AuthSession?>(null)
        private set
    var activeProfile by mutableStateOf<ViewerProfile?>(null)
        private set
    var subscription by mutableStateOf<ViewerSubscription?>(null)
    var bootstrapError by mutableStateOf<String?>(null)
        private set
    var isBusy by mutableStateOf(false)
        private set

    /** True when the catalogue is unavailable and only downloads should be used. */
    var isOfflineMode by mutableStateOf(false)
        private set

    /** Native plan / PPV paywall. */
    var paywallContext by mutableStateOf<PaywallContext?>(null)
        private set

    /** After a successful PPV unlock, resume this playback request. */
    var pendingPlaybackAfterUnlock by mutableStateOf<PlaybackRequest?>(null)
        private set
    private var shouldResumePlaybackAfterPaywall = false

    private var hasBootstrapped = false

    fun bootstrapIfNeeded() {
        if (hasBootstrapped) return
        hasBootstrapped = true
        bootstrap()
    }

    /** Enter Downloads-only mode without waiting on the network. */
    fun enterOfflineDownloads() {
        isOfflineMode = true
        bootstrapError = null
        val profile = SessionStore.lastProfile(getApplication())
        if (profile != null) {
            activeProfile = profile
            ApiClient.setViewerProfileCookie(profile.id)
        }
        route = Route.MAIN
    }

    private fun bootstrap() {
        viewModelScope.launch {
            route = Route.LOADING
            bootstrapError = null
            isOfflineMode = false
            ApiClient.setViewerProfileCookie(null)
            activeProfile = null

            val app = getApplication<Application>()
            val online = NetworkMonitor.hasTransport(app)
            val hasDownloads = runCatching { DownloadController.completedEntries().isNotEmpty() }.getOrDefault(false)
            val minimumSplashMs = if (online) 1600L else 600L
            val start = System.currentTimeMillis()

            if (!online) {
                waitRemainingSplash(start, minimumSplashMs)
                if (hasDownloads || SessionStore.hasAuthCookie()) {
                    enterOfflineDownloads()
                } else {
                    bootstrapError = "You're offline. Connect to the internet to sign in, or download titles while online to watch them later."
                    route = Route.SIGN_IN
                }
                return@launch
            }

            try {
                val s = withTimeout(10_000) { AuthService.fetchSession() }
                session = s
                if (s?.user != null) {
                    subscription = runCatching {
                        withTimeout(8_000) { ViewerApi.fetchSubscription() }
                    }.getOrNull()
                    runCatching { BillingService.refreshProducts() }
                }
                waitRemainingSplash(start, minimumSplashMs)
                if (s?.user != null) {
                    route = Route.PROFILES
                    if (needsPaymentAttention) presentPaywall(PaywallContext.Subscribe)
                } else {
                    route = Route.SIGN_IN
                }
            } catch (e: Exception) {
                session = null
                waitRemainingSplash(start, minimumSplashMs)
                if (hasDownloads || SessionStore.hasAuthCookie()) {
                    bootstrapError = when (e) {
                        is TimeoutCancellationException -> "Connection timed out — opening downloads."
                        else -> e.localizedMessage ?: "Couldn't reach Story Time — opening downloads."
                    }
                    enterOfflineDownloads()
                } else {
                    bootstrapError = e.localizedMessage ?: "Couldn't reach Story Time. Check your connection."
                    route = Route.SIGN_IN
                }
            }
        }
    }

    private suspend fun waitRemainingSplash(startMs: Long, minimumMs: Long) {
        val elapsed = System.currentTimeMillis() - startMs
        if (elapsed < minimumMs) delay(minimumMs - elapsed)
    }

    suspend fun signIn(email: String, password: String) {
        isBusy = true
        try {
            isOfflineMode = false
            val s = AuthService.signIn(email, password)
            session = s
            ApiClient.setViewerProfileCookie(null)
            activeProfile = null
            subscription = runCatching { ViewerApi.fetchSubscription() }.getOrNull()
            route = Route.PROFILES
            if (needsPaymentAttention) {
                presentPaywall(if (subscription == null) PaywallContext.Subscribe else PaywallContext.Reactivate)
            }
        } finally {
            isBusy = false
        }
    }

    suspend fun signUp(email: String, password: String, name: String?) {
        isBusy = true
        try {
            isOfflineMode = false
            val s = AuthService.signUp(email, password, name)
            session = s
            ApiClient.setViewerProfileCookie(null)
            activeProfile = null
            subscription = runCatching { ViewerApi.fetchSubscription() }.getOrNull()
            route = Route.PROFILES
            presentPaywall(PaywallContext.Subscribe)
        } finally {
            isBusy = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            isBusy = true
            AuthService.signOut()
            session = null
            activeProfile = null
            subscription = null
            paywallContext = null
            pendingPlaybackAfterUnlock = null
            isOfflineMode = false
            SessionStore.clearProfile(getApplication())
            ApiClient.setViewerProfileCookie(null)
            route = Route.SIGN_IN
            isBusy = false
        }
    }

    fun selectProfile(profile: ViewerProfile) {
        activeProfile = profile
        ApiClient.setViewerProfileCookie(profile.id)
        SessionStore.saveProfile(getApplication(), profile)
        isOfflineMode = false
        route = Route.MAIN
    }

    fun switchProfile() {
        if (isOfflineMode) return
        activeProfile = null
        ApiClient.setViewerProfileCookie(null)
        route = Route.PROFILES
    }

    fun presentPaywall(context: PaywallContext = PaywallContext.Subscribe) {
        if (isOfflineMode) return
        paywallContext = context
    }

    fun dismissPaywall() {
        paywallContext = null
    }

    fun presentPpvUnlock(contentId: String, title: String?, resume: PlaybackRequest? = null) {
        if (isOfflineMode) return
        pendingPlaybackAfterUnlock = resume
        shouldResumePlaybackAfterPaywall = false
        paywallContext = PaywallContext.Ppv(contentId, title)
    }

    fun markPpvUnlockSucceeded() {
        shouldResumePlaybackAfterPaywall = true
    }

    /** Call when paywall closes — returns a request only after a successful PPV purchase. */
    fun consumeResumePlaybackAfterUnlock(): PlaybackRequest? {
        val req = if (shouldResumePlaybackAfterPaywall) pendingPlaybackAfterUnlock else null
        shouldResumePlaybackAfterPaywall = false
        pendingPlaybackAfterUnlock = null
        return req
    }

    fun clearPendingPlaybackAfterUnlock() {
        pendingPlaybackAfterUnlock = null
        shouldResumePlaybackAfterPaywall = false
    }

    fun refreshSubscription() {
        if (isOfflineMode) return
        viewModelScope.launch {
            subscription = runCatching { ViewerApi.fetchSubscription() }.getOrNull()
        }
    }

    /** Gate play for PPV accounts — call before opening the player. */
    suspend fun resolvePlayAccess(request: PlaybackRequest): TitleAccessResult =
        ViewerApi.resolveTitleAccess(
            contentId = request.contentId,
            isPayPerViewAccount = isPayPerViewAccount,
            isTrailer = request.isTrailer,
        )

    val isPayPerViewAccount: Boolean
        get() = subscription?.isPayPerViewModel == true

    val hasActiveServerSubscription: Boolean
        get() {
            val status = subscription?.status?.uppercase() ?: return false
            return status in listOf("ACTIVE", "TRIALING", "PAID")
        }

    /**
     * Needs a package (subscribe or pick PPV). Active PPV accounts are fine —
     * they unlock titles per-film, not via monthly subscription.
     */
    val needsPaymentAttention: Boolean
        get() {
            if (isOfflineMode) return false
            if (session?.user == null) return false
            if (isPayPerViewAccount && hasActiveServerSubscription) return false
            if (hasActiveServerSubscription) return false
            val status = subscription?.status?.uppercase()
            if (status == null) return true
            return status in listOf("PAST_DUE", "CANCELED", "CANCELLED", "EXPIRED", "INACTIVE", "NONE") ||
                status.isEmpty()
        }
}
