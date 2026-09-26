package com.storytime.universe.data.parental

import android.content.Context
import com.storytime.universe.data.model.ContentItem

/**
 * Local parental controls + age assurance (iOS `ParentalControls` parity).
 * PIN stays on-device; maturity hints may sync from viewer settings.
 */
class ParentalControls private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    var isEnabled: Boolean
        get() = prefs.getBoolean(KEY_ENABLED, false)
        set(value) { prefs.edit().putBoolean(KEY_ENABLED, value).apply() }

    var maxMaturityAge: Int
        get() {
            val stored = prefs.getInt(KEY_MAX_AGE, 18)
            return if (stored > 0) stored else 18
        }
        set(value) { prefs.edit().putInt(KEY_MAX_AGE, value).apply() }

    var requirePinToSwitchProfile: Boolean
        get() = prefs.getBoolean(KEY_PIN_SWITCH, false)
        set(value) { prefs.edit().putBoolean(KEY_PIN_SWITCH, value).apply() }

    var requirePinForPlayer: Boolean
        get() = prefs.getBoolean(KEY_PIN_PLAYER, false)
        set(value) { prefs.edit().putBoolean(KEY_PIN_PLAYER, value).apply() }

    var blockDownloads: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_DOWNLOADS, false)
        set(value) { prefs.edit().putBoolean(KEY_BLOCK_DOWNLOADS, value).apply() }

    val hasPin: Boolean
        get() = !prefs.getString(KEY_PIN, null).isNullOrEmpty()

    fun setPin(pin: String) {
        val trimmed = pin.trim()
        if (trimmed.length == 4 && trimmed.all { it.isDigit() }) {
            prefs.edit().putString(KEY_PIN, trimmed).apply()
        }
    }

    fun clearPin() {
        prefs.edit().remove(KEY_PIN).apply()
    }

    fun verifyPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN, null)
        if (stored.isNullOrEmpty()) return true
        return stored == pin.trim()
    }

    fun effectiveMaxAge(profileAge: Int?): Int {
        val profileLimit = profileAge ?: 18
        if (!isEnabled) return profileLimit
        return minOf(profileLimit, maxMaturityAge)
    }

    fun allows(contentMinAge: Int?, profileAge: Int?): Boolean {
        val required = contentMinAge ?: 0
        return required <= effectiveMaxAge(profileAge)
    }

    fun filter(items: List<ContentItem>, profileAge: Int?): List<ContentItem> =
        items.filter { allows(it.minAge, profileAge) }

    val maturityLabel: String
        get() = when (maxMaturityAge) {
            in Int.MIN_VALUE..7 -> "Little Kids (up to 7)"
            in 8..12 -> "Kids (up to 12)"
            in 13..15 -> "Teen (up to 15)"
            in 16..17 -> "Young adult (up to 17)"
            else -> "Adult (unrestricted)"
        }

    fun applyRemoteMaturityHints(enabled: Boolean?, maxAge: Int?) {
        if (enabled != null) isEnabled = enabled
        if (maxAge != null && maxAge > 0) maxMaturityAge = maxAge
    }

    fun needsPinToSwitchProfile(): Boolean = isEnabled && requirePinToSwitchProfile && hasPin
    fun needsPinForPlayer(): Boolean = isEnabled && requirePinForPlayer && hasPin
    fun needsPinForDownloads(): Boolean = isEnabled && blockDownloads && hasPin

    companion object {
        private const val PREFS = "st_parental"
        private const val KEY_ENABLED = "parental.enabled"
        private const val KEY_PIN = "parental.pin"
        private const val KEY_MAX_AGE = "parental.maxMaturityAge"
        private const val KEY_PIN_SWITCH = "parental.requirePinToSwitch"
        private const val KEY_PIN_PLAYER = "parental.requirePinForPlayer"
        private const val KEY_BLOCK_DOWNLOADS = "parental.blockDownloads"

        @Volatile private var instance: ParentalControls? = null

        fun get(context: Context): ParentalControls =
            instance ?: synchronized(this) {
                instance ?: ParentalControls(context).also { instance = it }
            }
    }
}
