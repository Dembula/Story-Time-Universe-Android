package com.storytime.universe.data.download

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.DatabaseProvider
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import com.storytime.universe.data.AppConfig
import com.storytime.universe.data.network.ApiClient
import com.storytime.universe.data.network.ViewerApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.builtins.serializer
import java.io.File
import java.util.concurrent.Executors

/**
 * Offline download orchestration built on Media3 `DownloadManager`. Downloaded media lives in the
 * app-private cache and can only be played inside the app — the Android analogue of the iOS
 * `DownloadManager` (AVAssetDownloadURLSession). Cookies flow through the shared OkHttpClient.
 */
@OptIn(UnstableApi::class)
object DownloadController {

    const val CHANNEL_ID = "downloads"
    const val FOREGROUND_NOTIFICATION_ID = 1
    private const val DOWNLOAD_CONTENT_DIR = "downloads"

    private lateinit var appContext: Context

    lateinit var downloadCache: Cache
        private set
    private lateinit var databaseProvider: DatabaseProvider
    lateinit var httpDataSourceFactory: DataSource.Factory
        private set
    lateinit var downloadManager: DownloadManager
        private set
    lateinit var notificationHelper: DownloadNotificationHelper
        private set

    private val _entries = MutableStateFlow<List<DownloadEntry>>(emptyList())
    val entries: StateFlow<List<DownloadEntry>> = _entries

    fun init(context: Context) {
        if (::downloadManager.isInitialized) return
        appContext = context.applicationContext

        databaseProvider = StandaloneDatabaseProvider(appContext)
        val dir = File(appContext.filesDir, DOWNLOAD_CONTENT_DIR)
        downloadCache = SimpleCache(dir, NoOpCacheEvictor(), databaseProvider)
        httpDataSourceFactory = OkHttpDataSource.Factory(ApiClient.okHttp).setUserAgent(AppConfig.USER_AGENT)

        downloadManager = DownloadManager(
            appContext,
            databaseProvider,
            downloadCache,
            httpDataSourceFactory,
            Executors.newFixedThreadPool(3),
        ).apply {
            maxParallelDownloads = 2
        }
        notificationHelper = DownloadNotificationHelper(appContext, CHANNEL_ID)

        downloadManager.addListener(object : DownloadManager.Listener {
            override fun onInitialized(downloadManager: DownloadManager) = refresh()
            override fun onDownloadChanged(dm: DownloadManager, download: Download, e: Exception?) = refresh()
            override fun onDownloadRemoved(dm: DownloadManager, download: Download) = refresh()
        })
        refresh()
    }

    /** A read-through cache source: plays from disk when downloaded, otherwise streams. */
    fun cacheDataSourceFactory(): CacheDataSource.Factory =
        CacheDataSource.Factory()
            .setCache(downloadCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setCacheWriteDataSinkFactory(null) // read-only during playback
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

    fun entryFor(contentId: String, episodeId: String?): DownloadEntry? =
        _entries.value.firstOrNull { it.key == DownloadSpec.makeKey(contentId, episodeId) }

    fun entryForKey(key: String): DownloadEntry? = _entries.value.firstOrNull { it.key == key }

    fun completedEntries(): List<DownloadEntry> =
        _entries.value.filter { it.state == UiDownloadState.COMPLETED }
            .sortedByDescending { it.createdAtMs }

    fun activeEntries(): List<DownloadEntry> =
        _entries.value.filter {
            it.state == UiDownloadState.DOWNLOADING ||
                it.state == UiDownloadState.QUEUED ||
                it.state == UiDownloadState.FAILED
        }.sortedByDescending { it.createdAtMs }

    /** Resolves the stream and enqueues a download (fetches the playback bundle like iOS). */
    suspend fun startDownload(spec: DownloadSpec) {
        val existing = entryForKey(spec.key)?.state
        if (existing == UiDownloadState.COMPLETED ||
            existing == UiDownloadState.DOWNLOADING ||
            existing == UiDownloadState.QUEUED
        ) return

        val bundle = ViewerApi.fetchPlaybackBundle(spec.contentId, spec.episodeId, trailer = false)
        val url = bundle.streamUrl ?: return
        val isHls = bundle.isHls

        val builder = DownloadRequest.Builder(spec.key, Uri.parse(url))
            .setData(encodeMeta(spec.toMeta()))
        if (isHls) builder.setMimeType(MimeTypes.APPLICATION_M3U8)

        DownloadService.sendAddDownload(
            appContext,
            StoryTimeDownloadService::class.java,
            builder.build(),
            /* foreground = */ false,
        )
    }

    fun cancelDownload(key: String) = removeDownload(key)

    fun removeDownload(key: String) {
        DownloadService.sendRemoveDownload(
            appContext,
            StoryTimeDownloadService::class.java,
            key,
            /* foreground = */ false,
        )
    }

    fun removeDownload(contentId: String, episodeId: String?) =
        removeDownload(DownloadSpec.makeKey(contentId, episodeId))

    private fun refresh() {
        val list = ArrayList<DownloadEntry>()
        try {
            downloadManager.downloadIndex.getDownloads().use { cursor ->
                while (cursor.moveToNext()) {
                    val d = cursor.download
                    val meta = decodeMeta(d.request.data) ?: continue
                    val percent = if (d.percentDownloaded.isNaN()) 0f else d.percentDownloaded
                    list.add(
                        DownloadEntry(
                            key = d.request.id,
                            uri = d.request.uri.toString(),
                            meta = meta,
                            state = mapState(d.state),
                            progress = (percent / 100f).coerceIn(0f, 1f),
                            createdAtMs = d.startTimeMs,
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // ignore — surface whatever we could read
        }
        _entries.value = list
    }

    private fun mapState(state: Int): UiDownloadState = when (state) {
        Download.STATE_QUEUED -> UiDownloadState.QUEUED
        Download.STATE_DOWNLOADING -> UiDownloadState.DOWNLOADING
        Download.STATE_COMPLETED -> UiDownloadState.COMPLETED
        Download.STATE_FAILED -> UiDownloadState.FAILED
        Download.STATE_STOPPED -> UiDownloadState.PAUSED
        Download.STATE_REMOVING, Download.STATE_RESTARTING -> UiDownloadState.DOWNLOADING
        else -> UiDownloadState.QUEUED
    }

    private fun encodeMeta(meta: DownloadMeta): ByteArray =
        ApiClient.json.encodeToString(DownloadMeta.serializer(), meta).toByteArray(Charsets.UTF_8)

    private fun decodeMeta(data: ByteArray): DownloadMeta? =
        runCatching {
            ApiClient.json.decodeFromString(DownloadMeta.serializer(), String(data, Charsets.UTF_8))
        }.getOrNull()
}
