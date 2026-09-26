package com.storytime.universe

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.storytime.universe.ui.RootScreen
import com.storytime.universe.ui.player.PipController
import com.storytime.universe.ui.theme.StoryTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            StoryTimeTheme {
                RootScreen()
            }
        }
    }

    /** Auto-enter system mini player when the user leaves while video is playing (iOS parity). */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (PipController.isPlayerActive && !isInPictureInPictureMode) {
            PipController.enter(this)
        }
    }

    override fun onPause() {
        // Keep playback running in PiP — do not pause ExoPlayer from the Activity.
        super.onPause()
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        PipController.markInPip(isInPictureInPictureMode)
        if (!isInPictureInPictureMode) {
            PipController.syncParams(this)
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        PipController.markInPip(isInPictureInPictureMode)
    }
}

/** Finds the host Activity from a Compose LocalContext (used for orientation locking). */
fun Context.findActivity(): Activity? {
    var ctx = this
    while (ctx is ContextWrapper) {
        if (ctx is Activity) return ctx
        ctx = ctx.baseContext
    }
    return null
}

val isAtLeastAndroid13: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
