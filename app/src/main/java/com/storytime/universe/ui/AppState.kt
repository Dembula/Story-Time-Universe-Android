package com.storytime.universe.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.storytime.universe.data.network.ApiClient
import com.storytime.universe.data.network.AuthService
import com.storytime.universe.data.network.ViewerApi
import com.storytime.universe.data.model.AuthSession
import com.storytime.universe.data.model.ViewerProfile
import com.storytime.universe.data.model.ViewerSubscription
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

    private var hasBootstrapped = false

    fun bootstrapIfNeeded() {
        if (hasBootstrapped) return
        hasBootstrapped = true
        bootstrap()
    }

    /** Always land on profiles after auth — never auto-enter last profile on launch. */
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
                }
                waitRemainingSplash(start, minimumSplashMs)
                route = if (s?.user != null) Route.PROFILES else Route.SIGN_IN
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

    val needsPaymentAttention: Boolean
        get() {
            val status = subscription?.status?.uppercase() ?: return false
            return status in listOf("PAST_DUE", "CANCELED", "CANCELLED", "EXPIRED")
        }
}
