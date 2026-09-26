package com.storytime.universe.ui.player

import android.app.Activity
import android.app.PictureInPictureParams
import android.content.pm.PackageManager
import android.os.Build
import android.util.Rational
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Coordinates Activity Picture-in-Picture with [PlayerScreen], mirroring iOS
 * `PictureInPictureManager` (system mini player while the user leaves the app).
 */
object PipController {
    var isPlayerActive by mutableStateOf(false)
        private set
    var isInPip by mutableStateOf(false)
        private set

    private var aspectWidth = 16
    private var aspectHeight = 9

    fun markPlayerActive(active: Boolean) {
        isPlayerActive = active
        if (!active) isInPip = false
    }

    fun markInPip(inPip: Boolean) {
        isInPip = inPip
    }

    fun updateAspectRatio(width: Int, height: Int) {
        if (width > 0 && height > 0) {
            aspectWidth = width
            aspectHeight = height
        }
    }

    fun isSupported(activity: Activity): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        return activity.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
    }

    fun buildParams(): PictureInPictureParams? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(aspectWidth, aspectHeight).coercePip())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isPlayerActive)
            builder.setSeamlessResizeEnabled(true)
        }
        return builder.build()
    }

    fun enter(activity: Activity): Boolean {
        if (!isSupported(activity) || !isPlayerActive) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return false
        if (activity.isInPictureInPictureMode) return true
        val params = buildParams() ?: return false
        return try {
            activity.enterPictureInPictureMode(params)
        } catch (_: Exception) {
            false
        }
    }

    fun syncParams(activity: Activity) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (!isSupported(activity)) return
        val params = buildParams() ?: return
        try {
            activity.setPictureInPictureParams(params)
        } catch (_: Exception) {
            // ignore — some OEMs reject params until a surface exists
        }
    }

    private fun Rational.coercePip(): Rational {
        // Android requires PiP ratio between 0.418 and 2.39
        val value = toFloat()
        return when {
            value < 0.42f -> Rational(42, 100)
            value > 2.39f -> Rational(239, 100)
            else -> this
        }
    }
}
