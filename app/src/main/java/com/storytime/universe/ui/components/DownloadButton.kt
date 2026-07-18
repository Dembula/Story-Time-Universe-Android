package com.storytime.universe.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.storytime.universe.data.download.DownloadController
import com.storytime.universe.data.download.DownloadSpec
import com.storytime.universe.data.download.UiDownloadState
import com.storytime.universe.ui.theme.StColors
import kotlinx.coroutines.launch

enum class DownloadButtonStyle { ICON, LABELED }

@Composable
fun DownloadButton(spec: DownloadSpec, style: DownloadButtonStyle = DownloadButtonStyle.ICON) {
    val scope = rememberCoroutineScope()
    val entries by DownloadController.entries.collectAsState()
    val entry = entries.firstOrNull { it.key == spec.key }
    val state = entry?.state
    val progress = entry?.progress ?: 0f
    var confirmDelete by remember { mutableStateOf(false) }

    val onTap: () -> Unit = {
        when (state) {
            UiDownloadState.COMPLETED -> confirmDelete = true
            UiDownloadState.DOWNLOADING, UiDownloadState.QUEUED -> DownloadController.cancelDownload(spec.key)
            else -> scope.launch { runCatching { DownloadController.startDownload(spec) } }
        }
    }

    when (style) {
        DownloadButtonStyle.ICON -> Box(
            Modifier.size(40.dp).clickable { onTap() },
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                UiDownloadState.COMPLETED -> Icon(Icons.Filled.CheckCircle, "Downloaded", tint = StColors.Accent)
                UiDownloadState.DOWNLOADING, UiDownloadState.QUEUED -> Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        progress = { progress.coerceAtLeast(0.02f) },
                        color = StColors.Accent,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(26.dp),
                        strokeWidth = 2.5.dp,
                    )
                    Icon(Icons.Filled.Stop, null, tint = Color.White, modifier = Modifier.size(10.dp))
                }
                UiDownloadState.FAILED -> Icon(Icons.Filled.ErrorOutline, "Retry", tint = Color(0xFFFFA726))
                else -> Icon(Icons.Filled.Download, "Download", tint = Color.White.copy(alpha = 0.9f))
            }
        }

        DownloadButtonStyle.LABELED -> Row(
            Modifier
                .fillMaxWidth()
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.16f))
                .clickable { onTap() }
                .padding(vertical = 14.dp),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            when (state) {
                UiDownloadState.COMPLETED -> {
                    Icon(Icons.Filled.CheckCircle, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer8()
                    Text("Downloaded", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                UiDownloadState.DOWNLOADING, UiDownloadState.QUEUED -> {
                    CircularProgressIndicator(
                        progress = { progress.coerceAtLeast(0.02f) },
                        color = StColors.Accent,
                        trackColor = Color.White.copy(alpha = 0.25f),
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                    )
                    Spacer8()
                    Text("${(progress * 100).toInt()}%", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                UiDownloadState.FAILED -> {
                    Icon(Icons.Filled.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer8()
                    Text("Retry", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                else -> {
                    Icon(Icons.Filled.Download, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer8()
                    Text("Download", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            containerColor = StColors.Surface,
            title = { Text("Remove download?", color = StColors.Foreground) },
            text = { Text("This removes the offline copy from this device.", color = StColors.Muted) },
            confirmButton = {
                TextButton(onClick = {
                    DownloadController.removeDownload(spec.key)
                    confirmDelete = false
                }) { Text("Delete Download", color = Color(0xFFE5484D)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Cancel", color = StColors.Muted) }
            },
        )
    }
}

@Composable
private fun Spacer8() = androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
