package com.storytime.universe.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storytime.universe.data.billing.BillingService
import com.storytime.universe.data.billing.PaywallContext
import com.storytime.universe.data.network.ApiClient
import com.storytime.universe.data.network.AuthService
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.data.model.AuthSession
import com.storytime.universe.data.model.TitleAccessResult
import com.storytime.universe.data.model.ViewerProfile
import com.storytime.universe.data.model.ViewerSubscription
import com.storytime.universe.ui.player.PlaybackRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Top-level router + session state, ported from the iOS `AppState`. */
class AppState : ViewModel() {

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

    private fun bootstrap() {
        viewModelScope.launch {
            route = Route.LOADING
            bootstrapError = null
            ApiClient.setViewerProfileCookie(null)
            activeProfile = null

            val minimumSplashMs = 2800L
            val start = System.currentTimeMillis()

            try {
                val s = AuthService.fetchSession()
                session = s
                if (s?.user != null) {
                    subscription = runCatching { ViewerApi.fetchSubscription() }.getOrNull()
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
                bootstrapError = e.localizedMessage
                waitRemainingSplash(start, minimumSplashMs)
                route = Route.SIGN_IN
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
            ApiClient.setViewerProfileCookie(null)
            route = Route.SIGN_IN
            isBusy = false
        }
    }

    fun selectProfile(profile: ViewerProfile) {
        activeProfile = profile
        ApiClient.setViewerProfileCookie(profile.id)
        route = Route.MAIN
    }

    fun switchProfile() {
        activeProfile = null
        ApiClient.setViewerProfileCookie(null)
        route = Route.PROFILES
    }

    fun presentPaywall(context: PaywallContext = PaywallContext.Subscribe) {
        paywallContext = context
    }

    fun dismissPaywall() {
        paywallContext = null
    }

    fun presentPpvUnlock(contentId: String, title: String?, resume: PlaybackRequest? = null) {
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
            if (session?.user == null) return false
            if (isPayPerViewAccount && hasActiveServerSubscription) return false
            if (hasActiveServerSubscription) return false
            val status = subscription?.status?.uppercase()
            if (status == null) return true
            return status in listOf("PAST_DUE", "CANCELED", "CANCELLED", "EXPIRED", "INACTIVE", "NONE") ||
                status.isEmpty()
        }
}
