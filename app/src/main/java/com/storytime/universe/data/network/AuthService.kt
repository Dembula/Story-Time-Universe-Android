package com.storytime.universe.data.network

import com.storytime.universe.data.AppConfig
import com.storytime.universe.data.model.AuthSession
import com.storytime.universe.data.model.CsrfResponse
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** Ported from the iOS `AuthService` — NextAuth credentials flow over cookies. */
object AuthService {

    private val api = ApiClient

    suspend fun fetchSession(): AuthSession? {
        val result = api.request(path = "api/auth/session")
        if (!result.isSuccess) {
            if (result.code == 401) return null
            throw api.parseApiError(result)
        }
        val text = result.bodyString().trim()
        if (text.isEmpty() || text == "null" || text == "{}") return null
        val session = api.decode<AuthSession>(result)
        if (session.user?.email == null && session.user?.id == null) return null
        return session
    }

    suspend fun signIn(email: String, password: String): AuthSession {
        val csrfResult = api.request(path = "api/auth/csrf")
        if (!csrfResult.isSuccess) throw api.parseApiError(csrfResult)
        val csrf = api.decode<CsrfResponse>(csrfResult)

        val form = mapOf(
            "csrfToken" to csrf.csrfToken,
            "email" to email.trim().lowercase(),
            "password" to password,
            "callbackUrl" to (AppConfig.WEB_BASE_URL + "/profiles"),
            "json" to "true",
        )

        val result = api.request(
            path = "api/auth/callback/credentials-viewer",
            method = "POST",
            formBody = form,
            acceptsJson = true,
        )

        val bodyText = result.bodyString()
        val obj = runCatching { Json.parseToJsonElement(bodyText).jsonObject }.getOrNull()

        val explicitError = obj?.get("error")?.jsonPrimitive?.contentOrNullSafe()
        if (!explicitError.isNullOrEmpty()) {
            throw ApiException.Server(
                if (explicitError == "CredentialsSignin") "Invalid email or password." else explicitError
            )
        }

        // NextAuth (json:true) returns { url } — a url containing error= signals bad credentials.
        val url = obj?.get("url")?.jsonPrimitive?.contentOrNullSafe()
        if (url != null && url.contains("error=CredentialsSignin")) {
            throw ApiException.Server("Invalid email or password.")
        }

        if (!result.isSuccess && result.code != 302) {
            throw api.parseApiError(result)
        }

        val session = fetchSession()
        if (session?.user == null) {
            throw ApiException.Server("Sign-in succeeded but no session was created.")
        }
        return session
    }

    suspend fun signOut() {
        val csrfResult = runCatching { api.request(path = "api/auth/csrf") }.getOrNull()
        val csrf = csrfResult?.let { runCatching { api.decode<CsrfResponse>(it) }.getOrNull() }
        if (csrf != null) {
            runCatching {
                api.request(
                    path = "api/auth/signout",
                    method = "POST",
                    formBody = mapOf(
                        "csrfToken" to csrf.csrfToken,
                        "callbackUrl" to AppConfig.WEB_BASE_URL,
                        "json" to "true",
                    ),
                )
            }
        }
        api.clearCookies()
    }

    private fun kotlinx.serialization.json.JsonPrimitive.contentOrNullSafe(): String? =
        runCatching { this.content }.getOrNull()
}
