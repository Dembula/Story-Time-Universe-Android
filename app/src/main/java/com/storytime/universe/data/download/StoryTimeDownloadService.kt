package com.storytime.universe.data.download

import android.app.Notification
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.storytime.universe.R

/** Foreground service that runs Media3 offline downloads. */
@OptIn(UnstableApi::class)
class StoryTimeDownloadService : DownloadService(
    DownloadController.FOREGROUND_NOTIFICATION_ID,
    DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
    DownloadController.CHANNEL_ID,
    R.string.download_channel_name,
    0,
) {

    override fun getDownloadManager(): DownloadManager {
        DownloadController.init(applicationContext)
        return DownloadController.downloadManager
    }

    override fun getScheduler(): Scheduler? = null

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int,
    ): Notification {
        return DownloadController.notificationHelper.buildProgressNotification(
            this,
            R.drawable.ic_stat_download,
            null,
            null,
            downloads,
            notMetRequirements,
        )
    }
}
