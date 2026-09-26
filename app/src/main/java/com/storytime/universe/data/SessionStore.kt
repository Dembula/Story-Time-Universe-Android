package com.storytime.universe.data

import android.content.Context
import com.storytime.universe.data.model.ViewerProfile
import com.storytime.universe.data.network.ApiClient

/**
 * Lightweight persistence for offline resume (last profile + whether a session cookie exists).
 */
object SessionStore {
    private const val PREFS = "st_session"
    private const val KEY_PROFILE_ID = "profile_id"
    private const val KEY_PROFILE_NAME = "profile_name"
    private const val KEY_PROFILE_AGE = "profile_age"

    fun saveProfile(context: Context, profile: ViewerProfile) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_PROFILE_ID, profile.id)
            .putString(KEY_PROFILE_NAME, profile.name)
            .putInt(KEY_PROFILE_AGE, profile.age)
            .apply()
    }

    fun clearProfile(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
    }

    fun lastProfile(context: Context): ViewerProfile? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_PROFILE_ID, null) ?: return null
        val name = prefs.getString(KEY_PROFILE_NAME, null) ?: return null
        val age = prefs.getInt(KEY_PROFILE_AGE, 18)
        return ViewerProfile(id = id, name = name, age = age)
    }

    fun hasAuthCookie(): Boolean =
        runCatching { ApiClient.cookieJar.hasSessionCookie() }.getOrDefault(false)
}
