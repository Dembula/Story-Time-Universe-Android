package com.storytime.universe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** Brand palette matching Story Time Production viewer (`globals.css` + iOS `Theme`). */
object StColors {
    val Background = Color(0xFF000000)
    val Surface = Color(red = 0.03f, green = 0.03f, blue = 0.03f)
    val Card = Color(red = 0.04f, green = 0.04f, blue = 0.04f)
    val Foreground = Color(red = 0.96f, green = 0.96f, blue = 0.96f)
    val Muted = Color(red = 0.62f, green = 0.62f, blue = 0.62f)
    val Border = Color(1f, 1f, 1f, 0.12f)

    /** --primary: 31 100% 56% — orange brand accent */
    val Accent = Color(red = 1.0f, green = 0.575f, blue = 0.12f)
    /** --accent: 39 100% 64% — gold highlight */
    val AccentGold = Color(red = 1.0f, green = 0.77f, blue = 0.28f)
    val AccentSoft = Accent.copy(alpha = 0.15f)

    val PlayButton = Color.White
    val PlayButtonForeground = Color.Black
    val ProgressTrack = Color(1f, 1f, 1f, 0.25f)
    val NavInactive = Color(red = 0.64f, green = 0.64f, blue = 0.64f)

    private val profileColors = listOf(
        Color(red = 0.20f, green = 0.45f, blue = 0.95f),
        Color(red = 0.95f, green = 0.75f, blue = 0.15f),
        Color(red = 0.25f, green = 0.70f, blue = 0.55f),
        Color(red = 0.90f, green = 0.30f, blue = 0.35f),
        Color(red = 0.65f, green = 0.40f, blue = 0.90f),
        Color(red = 0.95f, green = 0.55f, blue = 0.20f),
    )

    fun profileColor(id: String): Color {
        val hash = abs(id.hashCode())
        return profileColors[hash % profileColors.size]
    }
}

private val StoryTimeColorScheme = darkColorScheme(
    primary = StColors.Accent,
    onPrimary = Color.Black,
    secondary = StColors.AccentGold,
    background = StColors.Background,
    onBackground = StColors.Foreground,
    surface = StColors.Surface,
    onSurface = StColors.Foreground,
    surfaceVariant = StColors.Card,
    error = Color(0xFFE5484D),
)

@Composable
fun StoryTimeTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION")
    isSystemInDarkTheme() // app is always dark, matching the iOS client
    MaterialTheme(
        colorScheme = StoryTimeColorScheme,
        typography = Typography(),
        content = content,
    )
}
