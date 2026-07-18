package com.storytime.universe

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.storytime.universe.data.download.DownloadController
import com.storytime.universe.data.network.ApiClient

class StoryTimeApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        ApiClient.init(this)
        DownloadController.init(this)
    }

    /** Coil loads images through the same authenticated OkHttp client so session-protected
     *  preview-proxy images (`/api/files/preview`) load with the viewer's cookies. */
    override fun newImageLoader(): ImageLoader =
        ImageLoader.Builder(this)
            .okHttpClient(ApiClient.okHttp)
            .crossfade(true)
            .memoryCache { MemoryCache.Builder(this).maxSizePercent(0.25).build() }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(150L * 1024 * 1024)
                    .build()
            }
            .build()
}
