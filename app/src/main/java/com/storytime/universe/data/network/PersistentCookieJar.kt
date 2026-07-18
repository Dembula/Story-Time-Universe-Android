package com.storytime.universe.data.network

import android.content.Context
import com.storytime.universe.data.AppConfig
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl

/**
 * Cookie jar that persists cookies to SharedPreferences so the NextAuth session survives
 * app restarts — the Android analogue of iOS's shared `HTTPCookieStorage`.
 */
class PersistentCookieJar(context: Context) : CookieJar {

    private val prefs = context.getSharedPreferences("st_cookies", Context.MODE_PRIVATE)
    private val store = LinkedHashMap<String, Cookie>()

    init {
        load()
    }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        var changed = false
        for (cookie in cookies) {
            val key = keyFor(cookie)
            if (cookie.expiresAt < System.currentTimeMillis() && !cookie.persistent) {
                // Session cookie with no explicit expiry — still keep in memory.
            }
            if (cookie.expiresAt <= System.currentTimeMillis() && cookie.persistent) {
                store.remove(key)
            } else {
                store[key] = cookie
            }
            changed = true
        }
        if (changed) persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val expired = mutableListOf<String>()
        val matches = store.values.filter { cookie ->
            if (cookie.persistent && cookie.expiresAt <= now) {
                expired.add(keyFor(cookie)); false
            } else {
                cookie.matches(url)
            }
        }
        if (expired.isNotEmpty()) {
            expired.forEach { store.remove(it) }
            persist()
        }
        return matches
    }

    @Synchronized
    fun clear() {
        store.clear()
        prefs.edit().clear().apply()
    }

    /** Sets or clears the active viewer profile cookie, matching iOS `setViewerProfileCookie`. */
    @Synchronized
    fun setViewerProfileCookie(profileId: String?) {
        val toRemove = store.values.filter {
            it.name == AppConfig.VIEWER_PROFILE_COOKIE_NAME ||
                it.name == AppConfig.VIEWER_PROFILE_UNLOCK_COOKIE_NAME
        }.map { keyFor(it) }
        toRemove.forEach { store.remove(it) }

        if (!profileId.isNullOrEmpty()) {
            val cookie = Cookie.Builder()
                .name(AppConfig.VIEWER_PROFILE_COOKIE_NAME)
                .value(profileId)
                .domain(AppConfig.API_HOST)
                .path("/")
                .secure()
                .build()
            store[keyFor(cookie)] = cookie
        }
        persist()
    }

    private fun keyFor(cookie: Cookie): String =
        "${cookie.name}|${cookie.domain}|${cookie.path}"

    private fun persist() {
        val serialized = store.values.mapNotNull { serialize(it) }.toSet()
        prefs.edit().putStringSet(KEY, serialized).apply()
    }

    private fun load() {
        val serialized = prefs.getStringSet(KEY, emptySet()) ?: return
        for (line in serialized) {
            deserialize(line)?.let { store[keyFor(it)] = it }
        }
    }

    private fun serialize(cookie: Cookie): String? = buildString {
        append(cookie.name).append('\u0001')
        append(cookie.value).append('\u0001')
        append(cookie.domain).append('\u0001')
        append(cookie.path).append('\u0001')
        append(cookie.expiresAt).append('\u0001')
        append(cookie.secure).append('\u0001')
        append(cookie.httpOnly).append('\u0001')
        append(cookie.hostOnly)
    }

    private fun deserialize(line: String): Cookie? {
        val parts = line.split('\u0001')
        if (parts.size < 8) return null
        return try {
            val builder = Cookie.Builder()
                .name(parts[0])
                .value(parts[1])
                .path(parts[3])
                .expiresAt(parts[4].toLong())
            if (parts[7].toBoolean()) builder.hostOnlyDomain(parts[2]) else builder.domain(parts[2])
            if (parts[5].toBoolean()) builder.secure()
            if (parts[6].toBoolean()) builder.httpOnly()
            builder.build()
        } catch (e: Exception) {
            null
        }
    }

    fun cookieHeaderFor(urlString: String): String? {
        val url = runCatching { urlString.toHttpUrl() }.getOrNull() ?: return null
        val cookies = loadForRequest(url)
        if (cookies.isEmpty()) return null
        return cookies.joinToString("; ") { "${it.name}=${it.value}" }
    }

    companion object {
        private const val KEY = "cookies"
    }
}
