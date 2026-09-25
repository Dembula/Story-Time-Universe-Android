package com.storytime.universe.data

/**
 * App-wide configuration mirroring the iOS client's `AppConfig`.
 * Payments & account management always open the production web app — never in-app.
 */
object AppConfig {
    /** Production viewer web app / API host. */
    const val WEB_BASE_URL = "https://story-time.online"
    const val API_BASE_URL = WEB_BASE_URL

    const val RENEW_SUBSCRIPTION_URL = "$WEB_BASE_URL/browse/account/renew"
    const val ACCOUNT_URL = "$WEB_BASE_URL/browse/account"
    const val CHANGE_PLAN_URL = "$WEB_BASE_URL/browse/account/change-plan"
    const val PACKAGE_ONBOARDING_URL = "$WEB_BASE_URL/onboarding/package"
    const val SIGN_UP_URL = "$WEB_BASE_URL/auth/signup"
    /** Marks Android app so web can adapt after payment (same pattern as iOS). */
    const val VIEWER_SIGN_UP_URL =
        "$WEB_BASE_URL/auth/signup?source=android_app&platform=android&callback=/profiles"
    const val FORGOT_PASSWORD_URL = "$WEB_BASE_URL/auth/forgot-password"
    const val TERMS_URL = "$WEB_BASE_URL/legal/terms"
    const val PRIVACY_URL = "$WEB_BASE_URL/legal/privacy"

    const val VIEWER_PROFILE_COOKIE_NAME = "st_viewer_profile"
    const val VIEWER_PROFILE_UNLOCK_COOKIE_NAME = "st_viewer_profile_unlock"

    const val USER_AGENT = "StoryTimeUniverseAndroid/1.0"

    val API_HOST: String
        get() = "story-time.online"

    /** NextAuth cookie names we treat as the session (used when clearing selectively). */
    val sessionCookieHints = listOf(
        "next-auth.session-token",
        "__Secure-next-auth.session-token",
        "next-auth.csrf-token",
        "__Host-next-auth.csrf-token",
    )
}
