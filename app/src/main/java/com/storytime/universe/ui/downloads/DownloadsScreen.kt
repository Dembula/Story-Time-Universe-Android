package com.storytime.universe.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DownloadDone
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.download.DownloadController
import com.storytime.universe.data.download.DownloadEntry
import com.storytime.universe.data.download.UiDownloadState
import com.storytime.universe.data.media.MediaUrl
import com.storytime.universe.data.parental.ParentalControls
import com.storytime.universe.ui.components.ParentalPinDialog
import com.storytime.universe.ui.components.RemoteImage
import com.storytime.universe.ui.main.NavActions
import com.storytime.universe.ui.player.PlaybackRequest
import com.storytime.universe.ui.theme.StColors

@Composable
fun DownloadsScreen(actions: NavActions) {
    val context = LocalContext.current
    val parental = remember { ParentalControls.get(context) }
    val entries by DownloadController.entries.collectAsState()
    val active = entries.filter {
        it.state == UiDownloadState.DOWNLOADING || it.state == UiDownloadState.QUEUED || it.state == UiDownloadState.FAILED || it.state == UiDownloadState.PAUSED
    }.sortedByDescending { it.createdAtMs }
    val completed = entries.filter { it.state == UiDownloadState.COMPLETED }.sortedByDescending { it.createdAtMs }
    var pendingPlay by remember { mutableStateOf<PlaybackRequest?>(null) }

    fun playEntry(entry: DownloadEntry) {
        val meta = entry.meta
        val request = PlaybackRequest(
            contentId = meta.contentId,
            title = meta.subtitle ?: meta.title,
            episodeId = meta.episodeId,
            offlineUrl = entry.uri,
        )
        if (parental.needsPinForPlayer() || parental.needsPinForDownloads()) {
            pendingPlay = request
        } else {
            actions.play(request)
        }
    }

    Column(Modifier.fillMaxSize().background(StColors.Background)) {
        Text("Downloads", color = StColors.Foreground, fontSize = 32.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(20.dp))

        if (entries.isEmpty()) {
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.DownloadDone, null, tint = StColors.Muted, modifier = Modifier.size(48.dp))
                Spacer(Modifier.size(12.dp))
                Text("No downloads yet", color = StColors.Foreground, fontSize = 18.sp)
                Text("Downloaded titles play offline, inside the app.", color = StColors.Muted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (active.isNotEmpty()) {
                    item { SectionLabel("In progress") }
                    items(active) { entry -> DownloadRow(entry, onPlay = { playEntry(it) }) }
                }
                if (completed.isNotEmpty()) {
                    item { SectionLabel("Available offline") }
                    items(completed) { entry -> DownloadRow(entry, onPlay = { playEntry(it) }) }
                }
            }
        }
    }

    pendingPlay?.let {
        ParentalPinDialog(
            title = "Play download",
            message = "Enter your parental PIN to play this download.",
            onDismiss = { pendingPlay = null },
            onSuccess = {
                val req = pendingPlay
                pendingPlay = null
                if (req != null) actions.play(req)
            },
        )
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.items(
    entries: List<DownloadEntry>,
    row: @Composable (DownloadEntry) -> Unit,
) {
    items(entries.size, key = { entries[it].key }) { row(entries[it]) }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text, color = StColors.Muted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun DownloadRow(entry: DownloadEntry, onPlay: (DownloadEntry) -> Unit) {
    val meta = entry.meta
    val playable = entry.state == UiDownloadState.COMPLETED
    val thumb = meta.posterUrl?.let { MediaUrl.resolve(posterUrl = it, videoUrl = null) }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.05f))
            .clickable(enabled = playable) { onPlay(entry) }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.width(112.dp).height(64.dp).clip(RoundedCornerShape(8.dp)).background(Color.White.copy(alpha = 0.08f)), contentAlignment = Alignment.Center) {
            if (thumb != null) RemoteImage(url = thumb, modifier = Modifier.fillMaxSize())
            when (entry.state) {
                UiDownloadState.COMPLETED -> Icon(Icons.Filled.PlayArrow, null, tint = Color.White)
                UiDownloadState.FAILED -> Icon(Icons.Filled.ErrorOutline, null, tint = Color(0xFFFFA726))
                else -> CircularProgressIndicator(color = StColors.Accent, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(meta.title, color = StColors.Foreground, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            meta.subtitle?.let { Text(it, color = StColors.Muted, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            when (entry.state) {
                UiDownloadState.COMPLETED -> Text("Available offline", color = StColors.Accent, fontSize = 11.sp)
                UiDownloadState.FAILED -> Text("Download failed · tap to remove", color = Color(0xFFFFA726), fontSize = 11.sp)
                else -> LinearProgressIndicator(
                    progress = { entry.progress.coerceAtLeast(0.02f) },
                    color = StColors.Accent,
                    trackColor = StColors.ProgressTrack,
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                )
            }
        }
        Icon(
            Icons.Filled.Delete,
            "Remove",
            tint = StColors.Muted,
            modifier = Modifier.size(22.dp).clickable { DownloadController.removeDownload(entry.key) },
        )
    }
}
