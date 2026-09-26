package com.storytime.universe.data.media

import android.content.Context
import coil.imageLoader
import coil.request.ImageRequest

/** Warms Coil memory/disk caches so home posters appear instantly after login. */
object ImagePrefetcher {
    fun prefetch(context: Context, urls: Iterable<String>, limit: Int = 48) {
        val loader = context.imageLoader
        urls.asSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(limit)
            .forEach { url ->
                loader.enqueue(
                    ImageRequest.Builder(context)
                        .data(url)
                        .memoryCachePolicy(coil.request.CachePolicy.ENABLED)
                        .diskCachePolicy(coil.request.CachePolicy.ENABLED)
                        .build()
                )
            }
    }
}
