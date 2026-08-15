package com.storytime.universe.data.network

import android.content.Context
import android.net.Uri
import com.storytime.universe.data.AppConfig
import com.storytime.universe.data.model.ApiErrorBody
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/** User-facing API errors, mirroring the iOS `APIError`. */
sealed class ApiException(message: String) : Exception(message) {
    object InvalidUrl : ApiException("Invalid request URL.")
    object Unauthorized : ApiException("Please sign in again.")
    class PaymentRequired(val info: String) : ApiException(info)
    class Server(val info: String) : ApiException(info)
    class Decoding(val info: String) : ApiException("Could not read server response: $info")
    class Network(val info: String) : ApiException(info)
}

data class HttpResult(val code: Int, val body: ByteArray) {
    fun bodyString(): String = String(body, Charsets.UTF_8)
    val isSuccess: Boolean get() = code in 200..299
}

/**
 * Cookie-based HTTP client for the Story Time viewer API. Ported from the iOS `APIClient`.
 * Initialized once from the Application with an app context so cookies persist.
 */
object ApiClient {

    val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
        explicitNulls = false
    }

    lateinit var cookieJar: PersistentCookieJar
        private set

    lateinit var okHttp: OkHttpClient
        private set

    fun init(context: Context) {
        if (::okHttp.isInitialized) return
        cookieJar = PersistentCookieJar(context.applicationContext)
        okHttp = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .callTimeout(120, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    fun clearCookies() = cookieJar.clear()

    fun setViewerProfileCookie(profileId: String?) = cookieJar.setViewerProfileCookie(profileId)

    fun absoluteUrl(path: String, query: List<Pair<String, String>> = emptyList()): String? {
        val cleaned = path.trim('/')
        val base = AppConfig.API_BASE_URL.trimEnd('/') + "/" + cleaned
        return try {
            if (query.isEmpty()) base
            else {
                val builder = Uri.parse(base).buildUpon()
                query.forEach { (k, v) -> builder.appendQueryParameter(k, v) }
                builder.build().toString()
            }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun request(
        path: String,
        method: String = "GET",
        query: List<Pair<String, String>> = emptyList(),
        jsonBody: Map<String, Any?>? = null,
        formBody: Map<String, String>? = null,
        acceptsJson: Boolean = true,
    ): HttpResult = withContext(Dispatchers.IO) {
        val url = absoluteUrl(path, query) ?: throw ApiException.InvalidUrl

        val builder = Request.Builder()
            .url(url)
            .header("User-Agent", AppConfig.USER_AGENT)
            .header("X-ST-Platform", "android")
        if (acceptsJson) builder.header("Accept", "application/json")

        // Explicit profile header so watch sessions attribute correctly alongside the cookie.
        cookieJar.viewerProfileId()?.takeIf { it.isNotEmpty() }?.let {
            builder.header("X-ST-Viewer-Profile", it)
        }

        val body = when {
            jsonBody != null -> {
                val payload = encodeJsonBody(jsonBody)
                payload.toRequestBody("application/json".toMediaType())
            }
            formBody != null -> {
                val encoded = formBody.entries.joinToString("&") { (k, v) ->
                    "${encode(k)}=${encode(v)}"
                }
                encoded.toRequestBody("application/x-www-form-urlencoded".toMediaType())
            }
            else -> null
        }

        builder.method(method, body ?: if (method == "GET" || method == "HEAD") null else ByteArray(0).toRequestBody())

        try {
            okHttp.newCall(builder.build()).execute().use { response ->
                val bytes = response.body?.bytes() ?: ByteArray(0)
                HttpResult(response.code, bytes)
            }
        } catch (e: Exception) {
            throw ApiException.Network(e.localizedMessage ?: "Network error.")
        }
    }

    inline fun <reified T> decode(result: HttpResult): T {
        return try {
            json.decodeFromString<T>(result.bodyString())
        } catch (e: Exception) {
            throw ApiException.Decoding(e.localizedMessage ?: "decode failure")
        }
    }

    fun parseApiError(result: HttpResult): ApiException {
        val body = runCatching { json.decodeFromString<ApiErrorBody>(result.bodyString()) }.getOrNull()
        val message = body?.error
        if (message != null) {
            if (result.code == 402 || body.paymentRequired == true) return ApiException.PaymentRequired(message)
            if (result.code == 401) return ApiException.Unauthorized
            return ApiException.Server(message)
        }
        if (result.code == 401) return ApiException.Unauthorized
        if (result.code == 402) return ApiException.PaymentRequired("Complete your subscription on the web.")
        return ApiException.Server("Request failed (${result.code}).")
    }

    /** Encodes a JSON object from a simple map (String/Int/Double/Boolean/null values). */
    fun encodeJsonBody(map: Map<String, Any?>): String {
        val sb = StringBuilder("{")
        var first = true
        for ((k, v) in map) {
            if (!first) sb.append(",")
            first = false
            sb.append(jsonString(k)).append(":")
            sb.append(
                when (v) {
                    null -> "null"
                    is String -> jsonString(v)
                    is Boolean -> v.toString()
                    is Int -> v.toString()
                    is Long -> v.toString()
                    is Double -> v.toString()
                    is Float -> v.toString()
                    else -> jsonString(v.toString())
                }
            )
        }
        sb.append("}")
        return sb.toString()
    }

    private fun jsonString(value: String): String {
        val sb = StringBuilder("\"")
        for (c in value) {
            when (c) {
                '\\' -> sb.append("\\\\")
                '"' -> sb.append("\\\"")
                '\n' -> sb.append("\\n")
                '\r' -> sb.append("\\r")
                '\t' -> sb.append("\\t")
                else -> sb.append(c)
            }
        }
        sb.append("\"")
        return sb.toString()
    }

    private fun encode(value: String): String =
        URLEncoder.encode(value, "UTF-8").replace("+", "%20")
}
