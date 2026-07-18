package com.storytime.universe.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.storytime.universe.ui.theme.StColors

/**
 * Loads remote images with an ordered multi-URL fallback (ported from the iOS `RemoteImage`).
 * Tries each candidate in order; on error advances to the next; shows a branded placeholder.
 */
@Composable
fun RemoteImage(
    urls: List<String>,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    var index by remember(urls) { mutableIntStateOf(0) }

    Box(
        modifier = modifier.background(
            Brush.linearGradient(
                listOf(
                    Color(red = 0.12f, green = 0.12f, blue = 0.14f),
                    Color(red = 0.06f, green = 0.06f, blue = 0.07f),
                )
            )
        ),
        contentAlignment = Alignment.Center,
    ) {
        val current = urls.getOrNull(index)
        var showPlaceholder = current == null

        if (current != null) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(context)
                    .data(current)
                    .crossfade(true)
                    .build()
            )
            val state = painter.state
            LaunchedEffect(state, index, urls) {
                if (state is AsyncImagePainter.State.Error && index < urls.lastIndex) {
                    index++
                }
            }
            if (state is AsyncImagePainter.State.Error && index >= urls.lastIndex) {
                showPlaceholder = true
            }
            Image(
                painter = painter,
                contentDescription = null,
                contentScale = contentScale,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (showPlaceholder) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = null,
                tint = StColors.Muted.copy(alpha = 0.55f),
            )
        }
    }
}

@Composable
fun RemoteImage(
    url: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) = RemoteImage(urls = url?.let { listOf(it) } ?: emptyList(), modifier = modifier, contentScale = contentScale)
