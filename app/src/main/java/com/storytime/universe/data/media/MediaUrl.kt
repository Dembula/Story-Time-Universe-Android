package com.storytime.universe.data.media

import android.net.Uri
import com.storytime.universe.data.AppConfig

/**
 * Ordered image-candidate resolver. Ported 1:1 from the iOS `MediaURL` enum so the Android
 * client resolves the exact same poster/backdrop sources, S3 preview-proxy URLs, and
 * Cloudflare Stream thumbnails.
 */
object MediaUrl {

    fun candidates(
        posterUrl: String?,
        backdropUrl: String? = null,
        videoUrl: String? = null,
        preferBackdrop: Boolean = false,
    ): List<String> {
        val seen = LinkedHashSet<String>()
        val result = ArrayList<String>()

        fun append(url: String?) {
            if (url.isNullOrEmpty()) return
            if (seen.add(url)) result.add(url)
        }

        val primary = if (preferBackdrop) backdropUrl else posterUrl
        val secondary = if (preferBackdrop) posterUrl else backdropUrl
        val backdropKey = normalizedKey(backdropUrl)

        if (preferBackdrop) {
            append(displayableHttpUrl(primary))
            append(previewProxyUrl(primary))
            append(siteRelativeUrl(primary))
            append(streamThumbnailUrl(videoUrl, time = "5s", height = 720, width = null))
            append(displayableHttpUrl(secondary))
            append(previewProxyUrl(secondary))
            append(siteRelativeUrl(secondary))
        } else {
            append(posterOnly(displayableHttpUrl(primary), backdropKey))
            append(posterOnly(previewProxyUrl(primary), backdropKey))
            append(posterOnly(siteRelativeUrl(primary), backdropKey))

            val hasPosterArt = !posterUrl?.trim().isNullOrEmpty()
            if (!hasPosterArt) {
                append(streamThumbnailUrl(videoUrl, time = "2s", height = 480, width = 320))
            }
        }

        return if (result.size > 4) result.take(4) else result
    }

    fun resolve(
        posterUrl: String?,
        backdropUrl: String? = null,
        videoUrl: String? = null,
        preferBackdrop: Boolean = false,
    ): String? = candidates(posterUrl, backdropUrl, videoUrl, preferBackdrop).firstOrNull()

    fun displayableHttpUrl(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val lower = value.lowercase()
        if (!lower.startsWith("https://") && !lower.startsWith("http://")) return null
        if (isNonImageMediaUrl(value)) return null
        return value
    }

    fun httpUrl(raw: String?): String? = displayableHttpUrl(raw) ?: siteRelativeUrl(raw)

    fun siteRelativeUrl(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        if (value.startsWith("/")) {
            return AppConfig.API_BASE_URL.trimEnd('/') + value
        }
        return null
    }

    fun previewProxyUrl(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val lower = value.lowercase()
        val looksPrivate = value.startsWith("s3://") ||
            value.contains(".amazonaws.com/") ||
            value.contains("r2.cloudflarestorage.com") ||
            value.contains("storage.googleapis.com") ||
            (!lower.startsWith("http://") && !lower.startsWith("https://") && !value.startsWith("/"))
        val shouldProxy = looksPrivate || value.startsWith("s3://")
        if (!shouldProxy) return null
        return Uri.parse(AppConfig.API_BASE_URL.trimEnd('/') + "/api/files/preview")
            .buildUpon()
            .appendQueryParameter("ref", value)
            .appendQueryParameter("context", "marketplace")
            .build()
            .toString()
    }

    fun streamThumbnailUrl(
        videoUrl: String?,
        time: String = "3s",
        height: Int = 480,
        width: Int? = null,
    ): String? {
        val uid = extractStreamUid(videoUrl) ?: return null
        val builder = Uri.parse("https://videodelivery.net/$uid/thumbnails/thumbnail.jpg")
            .buildUpon()
            .appendQueryParameter("time", time)
            .appendQueryParameter("height", height.toString())
        if (width != null) {
            builder.appendQueryParameter("width", width.toString())
            builder.appendQueryParameter("fit", "crop")
        }
        return builder.build().toString()
    }

    fun extractStreamUid(videoUrl: String?): String? {
        val value = videoUrl?.trim().orEmpty()
        if (value.isEmpty()) return null

        if (Regex("^[a-fA-F0-9]{32}$").matches(value)) return value

        val uri = runCatching { Uri.parse(value) }.getOrNull()
        val host = uri?.host?.lowercase()
        if (host == null) {
            if (value.contains("videodelivery.net") || value.contains("cloudflarestream.com")) {
                return extractStreamUid("https://$value")
            }
            return null
        }
        if (!host.contains("videodelivery.net") && !host.contains("cloudflarestream.com")) return null
        val parts = uri.path.orEmpty().split("/").filter { it.isNotEmpty() }
        val first = parts.firstOrNull() ?: return null
        if (first.contains(".")) return null
        return first
    }

    private fun posterOnly(url: String?, backdropKey: String?): String? {
        if (url == null) return null
        if (!backdropKey.isNullOrEmpty() && normalizedKey(url) == backdropKey) return null
        return url
    }

    private fun normalizedKey(raw: String?): String? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        return value.lowercase()
    }

    private fun isNonImageMediaUrl(raw: String): Boolean {
        val lower = raw.lowercase()
        return lower.contains("manifest/video") ||
            lower.endsWith(".m3u8") ||
            lower.endsWith(".mpd") ||
            lower.endsWith(".mp4") ||
            lower.contains("/iframe") ||
            lower.contains("/downloads/")
    }
}
