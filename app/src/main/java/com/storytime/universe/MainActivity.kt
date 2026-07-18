package com.storytime.universe

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import com.storytime.universe.ui.RootScreen
import com.storytime.universe.ui.theme.StoryTimeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            StoryTimeTheme {
                RootScreen()
            }
        }
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
