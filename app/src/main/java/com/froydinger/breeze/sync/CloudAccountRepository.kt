package com.froydinger.breeze.sync

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.browser.customtabs.CustomTabsIntent
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.froydinger.breeze.data.EncryptedStateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.security.SecureRandom

data class CloudSyncPreferences(
    val bookmarks: Boolean = false,
    val tabs: Boolean = false,
    val history: Boolean = false,
    val chats: Boolean = false,
    val reminders: Boolean = false,
) {
    fun enabled(collection: String): Boolean = when (collection) {
        "bookmarks" -> bookmarks
        "tabs" -> tabs
        "history" -> history
        "chats" -> chats
        "reminders" -> reminders
        else -> false
    }

    fun with(collection: String, enabled: Boolean): CloudSyncPreferences = when (collection) {
        "bookmarks" -> copy(bookmarks = enabled)
        "tabs" -> copy(tabs = enabled)
        "history" -> copy(history = enabled)
        "chats" -> copy(chats = enabled)
        "reminders" -> copy(reminders = enabled)
        else -> this
    }

    fun toJson(userId: String) = JSONObject()
        .put("user_id", userId)
        .put("bookmarks", bookmarks)
        .put("tabs", tabs)
        .put("history", history)
        .put("chats", chats)
        .put("reminders", reminders)

    companion object {
        val COLLECTIONS = listOf("bookmarks", "tabs", "history", "chats", "reminders")
        fun fromJson(value: JSONObject?) = CloudSyncPreferences(
            bookmarks = value?.optBoolean("bookmarks") ?: false,
            tabs = value?.optBoolean("tabs") ?: false,
            history = value?.optBoolean("history") ?: false,
            chats = value?.optBoolean("chats") ?: false,
            reminders = value?.optBoolean("reminders") ?: false,
        )
    }
}

data class CloudPasskey(
    val id: String,
    val friendlyName: String,
    val createdAt: String,
)

/** Supabase Auth/PostgREST client. The app only embeds a public key; user sessions are encrypted locally. */
class CloudAccountRepository(
    context: Context,
    private val projectUrl: String,
    private val publicKey: String,
    private val redirectUri: String,
) {
    private val appContext = context.applicationContext
    private val sessionStore = EncryptedStateStore(
        appContext,
        fileName = "breeze_account_session.enc",
        keyAlias = "com.froydinger.breeze.account.session.v1",
    )
    private var session: JSONObject = runCatching { sessionStore.load() }.getOrDefault(JSONObject())

    var email by mutableStateOf(session.optString("email").takeIf(String::isNotBlank))
        private set
    var signedIn by mutableStateOf(
        session.optString("access_token").isNotBlank() && session.optString("user_id").isNotBlank(),
    )
        private set
    var preferences by mutableStateOf(CloudSyncPreferences.fromJson(session.optJSONObject("sync_preferences")))
        private set
    var syncStatus by mutableStateOf("Not synced yet")
    var configured: Boolean = projectUrl.startsWith("https://") && publicKey.isNotBlank()
        private set

    val userId: String? get() = session.optString("user_id").takeIf(String::isNotBlank)

    suspend fun signUp(address: String, password: String): String = withContext(Dispatchers.IO) {
        ensureConfigured()
        require(address.contains('@')) { "Enter a valid email address." }
        require(password.length >= 8) { "Use a password with at least 8 characters." }
        val verifier = makeVerifier()
        savePendingVerifier(verifier)
        val challenge = challenge(verifier)
        val callback = URLEncoder.encode(redirectUri, Charsets.UTF_8.name())
        val url = "/auth/v1/signup?redirect_to=$callback&code_challenge=$challenge&code_challenge_method=s256"
        val response = request("POST", url, JSONObject().put("email", address.trim()).put("password", password), null)
        val accessToken = response.optString("access_token")
        if (accessToken.isNotBlank()) saveSession(response)
        "Check your email to confirm your account. When you tap the link, Breeze will finish signing you in."
    }

    suspend fun signIn(address: String, password: String) = withContext(Dispatchers.IO) {
        ensureConfigured()
        val response = request(
            "POST",
            "/auth/v1/token?grant_type=password",
            JSONObject().put("email", address.trim()).put("password", password),
            null,
        )
        saveSession(response)
        preferences = fetchPreferences()
    }

    fun beginGoogleSignIn(activity: Activity) {
        ensureConfigured()
        val verifier = makeVerifier()
        savePendingVerifier(verifier)
        val challenge = challenge(verifier)
        val callback = URLEncoder.encode(redirectUri, Charsets.UTF_8.name())
        val url = "$projectUrl/auth/v1/authorize?provider=google&redirect_to=$callback&code_challenge=$challenge&code_challenge_method=s256"
        CustomTabsIntent.Builder().build().launchUrl(activity, Uri.parse(url))
    }

    suspend fun registerPasskey(activity: Activity): CloudPasskey {
        check(signedIn) { "Sign in before adding a passkey." }
        val challenge = request("POST", "/auth/v1/passkeys/registration/options", JSONObject(), accessToken())
        val challengeId = challenge.optString("challenge_id").takeIf(String::isNotBlank)
            ?: error("Breeze could not start passkey setup.")
        val options = challenge.optJSONObject("options") ?: error("Breeze received invalid passkey setup options.")
        val manager = CredentialManager.create(activity)
        val response = manager.createCredential(
            activity,
            CreatePublicKeyCredentialRequest(options.toString()),
        ) as? CreatePublicKeyCredentialResponse ?: error("The passkey provider returned an unsupported credential.")
        val credential = runCatching { JSONObject(response.registrationResponseJson) }
            .getOrElse { error("The passkey provider returned an invalid response.") }
        val verified = request(
            "POST",
            "/auth/v1/passkeys/registration/verify",
            JSONObject().put("challenge_id", challengeId).put("credential", credential),
            accessToken(),
        )
        return CloudPasskey(
            id = verified.optString("id"),
            friendlyName = verified.optString("friendly_name").ifBlank { "Passkey" },
            createdAt = verified.optString("created_at"),
        )
    }

    suspend fun signInWithPasskey(activity: Activity) {
        ensureConfigured()
        val challenge = request("POST", "/auth/v1/passkeys/authentication/options", JSONObject(), null)
        val challengeId = challenge.optString("challenge_id").takeIf(String::isNotBlank)
            ?: error("Breeze could not start passkey sign-in.")
        val options = challenge.optJSONObject("options") ?: error("Breeze received invalid passkey sign-in options.")
        val response = CredentialManager.create(activity).getCredential(
            activity,
            GetCredentialRequest(listOf(GetPublicKeyCredentialOption(options.toString()))),
        )
        val publicKey = response.credential as? PublicKeyCredential
            ?: error("Choose a passkey to sign in to Breeze.")
        val credential = runCatching { JSONObject(publicKey.authenticationResponseJson) }
            .getOrElse { error("The passkey provider returned an invalid response.") }
        val sessionResponse = request(
            "POST",
            "/auth/v1/passkeys/authentication/verify",
            JSONObject().put("challenge_id", challengeId).put("credential", credential),
            null,
        )
        saveSession(sessionResponse)
        preferences = fetchPreferences()
    }

    suspend fun loadPasskeys(): List<CloudPasskey> {
        if (!signedIn) return emptyList()
        val response = request("GET", "/auth/v1/passkeys", null, accessToken())
        val rows = response.optJSONArray("passkeys") ?: response.optJSONArray("__array") ?: JSONArray()
        return buildList {
            for (index in 0 until rows.length()) {
                val row = rows.optJSONObject(index) ?: continue
                val id = row.optString("id").takeIf(String::isNotBlank) ?: continue
                add(CloudPasskey(id, row.optString("friendly_name").ifBlank { "Passkey" }, row.optString("created_at")))
            }
        }
    }

    suspend fun deletePasskey(passkeyId: String) {
        require(passkeyId.matches(Regex("[A-Fa-f0-9-]{20,64}"))) { "Invalid passkey." }
        request("DELETE", "/auth/v1/passkeys/${URLEncoder.encode(passkeyId, Charsets.UTF_8.name())}", null, accessToken())
    }

    suspend fun finishAuthCallback(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        val fragment = uri.fragment.orEmpty().split('&').mapNotNull { part ->
            val pair = part.split('=', limit = 2)
            if (pair.size == 2) pair[0] to Uri.decode(pair[1]) else null
        }.toMap()
        val authError = uri.getQueryParameter("error_description")
            ?: uri.getQueryParameter("error")
            ?: fragment["error_description"]
            ?: fragment["error"]
        if (!authError.isNullOrBlank()) throw IllegalStateException(authError.take(220))

        val code = uri.getQueryParameter("code")
        if (!code.isNullOrBlank()) {
            val verifier = session.optString("pending_verifier")
            require(verifier.isNotBlank()) { "This sign-in link was started on another device. Start again on this phone." }
            val response = request(
                "POST",
                "/auth/v1/token?grant_type=pkce",
                JSONObject().put("auth_code", code).put("code_verifier", verifier),
                null,
            )
            saveSession(response)
            preferences = fetchPreferences()
            return@withContext true
        }

        // Supabase can return tokens in a fragment for confirmation links created before PKCE is enabled.
        val tokenHash = uri.getQueryParameter("token_hash")
        if (!tokenHash.isNullOrBlank()) {
            val type = uri.getQueryParameter("type").orEmpty()
            require(type in EMAIL_TOKEN_TYPES) { "This email confirmation link is not valid. Request a new link." }
            val response = request("POST", "/auth/v1/verify",
                JSONObject().put("token_hash", tokenHash).put("type", type), null)
            saveSession(response)
            preferences = fetchPreferences()
            return@withContext true
        }

        val accessToken = fragment["access_token"].orEmpty()
        if (accessToken.isBlank()) return@withContext false
        val user = request("GET", "/auth/v1/user", null, accessToken)
        val tokenResponse = JSONObject()
            .put("access_token", accessToken)
            .put("refresh_token", fragment["refresh_token"].orEmpty())
            .put("expires_in", fragment["expires_in"]?.toLongOrNull() ?: 3600L)
            .put("user", user)
        saveSession(tokenResponse)
        preferences = fetchPreferences()
        true
    }

    suspend fun loadPreferences() {
        if (signedIn) preferences = fetchPreferences()
    }

    suspend fun setPreference(collection: String, enabled: Boolean) {
        require(collection in CloudSyncPreferences.COLLECTIONS)
        val next = preferences.with(collection, enabled)
        val id = userId ?: error("Sign in to change cloud sync.")
        request("POST", "/rest/v1/breeze_sync_preferences?on_conflict=user_id", next.toJson(id), accessToken(),
            "resolution=merge-duplicates,return=minimal")
        cachePreferences(next, id)
    }

    suspend fun loadCollection(collection: String): JSONObject? {
        require(collection in CloudSyncPreferences.COLLECTIONS)
        val id = userId ?: error("Sign in to sync.")
        val query = "/rest/v1/breeze_sync_collections?select=payload&user_id=eq.$id&collection=eq.$collection&limit=1"
        val result = request("GET", query, null, accessToken())
        val rows = result.optJSONArray("__array") ?: JSONArray()
        return rows.optJSONObject(0)?.optJSONObject("payload")
    }

    suspend fun saveCollection(collection: String, payload: JSONObject) {
        require(collection in CloudSyncPreferences.COLLECTIONS)
        val id = userId ?: error("Sign in to sync.")
        val row = JSONObject().put("user_id", id).put("collection", collection).put("payload", payload)
        request("POST", "/rest/v1/breeze_sync_collections?on_conflict=user_id,collection", row, accessToken(),
            "resolution=merge-duplicates,return=minimal")
    }

    suspend fun setPushDevice(installationId: String, enabled: Boolean) {
        require(installationId.matches(Regex("[A-Za-z0-9_-]{20,64}"))) { "Invalid Firebase installation id." }
        if (enabled) {
            require(signedIn) { "Sign in before enabling reminder alerts on this device." }
            require(preferences.reminders) { "Turn on reminder sync before enabling push alerts." }
        }
        request(
            "POST",
            "/functions/v1/register-push-device",
            JSONObject().put("installationId", installationId).put("enabled", enabled),
            accessToken(),
        )
    }

    suspend fun loadAllCloudCollections(): Map<String, JSONObject> {
        if (!signedIn) return emptyMap()
        return CloudSyncPreferences.COLLECTIONS.mapNotNull { collection ->
            runCatching { loadCollection(collection)?.let { collection to it } }.getOrNull()
        }.toMap()
    }

    suspend fun deleteAccount() = withContext(Dispatchers.IO) {
        val token = accessToken()
        request("POST", "/functions/v1/delete-account", JSONObject(), token)
        clearSession()
        preferences = CloudSyncPreferences()
    }

    suspend fun signOut() {
        if (signedIn) runCatching { request("POST", "/auth/v1/logout", JSONObject(), accessToken()) }
        clearSession()
        preferences = CloudSyncPreferences()
    }

    private suspend fun fetchPreferences(): CloudSyncPreferences {
        val id = userId ?: return CloudSyncPreferences()
        val query = "/rest/v1/breeze_sync_preferences?select=*&user_id=eq.$id&limit=1"
        val rows = request("GET", query, null, accessToken()).optJSONArray("__array") ?: JSONArray()
        val row = rows.optJSONObject(0)
        if (row == null) {
            val defaults = CloudSyncPreferences()
            request("POST", "/rest/v1/breeze_sync_preferences?on_conflict=user_id", defaults.toJson(id), accessToken(),
                "resolution=merge-duplicates,return=minimal")
            return cachePreferences(defaults, id)
        }
        return cachePreferences(CloudSyncPreferences.fromJson(row), id)
    }

    private suspend fun accessToken(): String {
        ensureConfigured()
        val current = session.optString("access_token")
        require(current.isNotBlank()) { "Sign in to your Breeze account." }
        val expiresAt = session.optLong("expires_at", 0L)
        if (expiresAt > System.currentTimeMillis() + 60_000L) return current
        val refreshToken = session.optString("refresh_token")
        require(refreshToken.isNotBlank()) { "Your sign-in expired. Sign in again." }
        val result = request("POST", "/auth/v1/token?grant_type=refresh_token", JSONObject().put("refresh_token", refreshToken), null)
        saveSession(result)
        return session.getString("access_token")
    }

    private fun saveSession(value: JSONObject) {
        val user = value.optJSONObject("user") ?: value
        val current = JSONObject()
            .put("access_token", value.optString("access_token"))
            .put("refresh_token", value.optString("refresh_token"))
            .put("expires_at", System.currentTimeMillis() + value.optLong("expires_in", 3600L) * 1000L)
            .put("user_id", user.optString("id"))
            .put("email", user.optString("email"))
        session.optJSONObject("sync_preferences")?.let { current.put("sync_preferences", JSONObject(it.toString())) }
        session = current
        email = current.optString("email").takeIf(String::isNotBlank)
        signedIn = current.optString("access_token").isNotBlank() && current.optString("user_id").isNotBlank()
        sessionStore.save(current)
    }

    private fun savePendingVerifier(verifier: String) {
        val next = JSONObject(session.toString()).put("pending_verifier", verifier)
        session = next
        sessionStore.save(next)
    }

    private fun clearSession() {
        session = JSONObject()
        email = null
        signedIn = false
        preferences = CloudSyncPreferences()
        sessionStore.save(session)
    }

    private fun cachePreferences(value: CloudSyncPreferences, id: String): CloudSyncPreferences {
        preferences = value
        session = JSONObject(session.toString()).put("sync_preferences", value.toJson(id))
        sessionStore.save(session)
        return value
    }

    private fun ensureConfigured() {
        check(configured) { "Breeze account sign-in is not configured in this build yet." }
    }

    private suspend fun request(
        method: String,
        path: String,
        body: JSONObject?,
        bearer: String?,
        prefer: String? = null,
    ): JSONObject = withContext(Dispatchers.IO) {
        ensureConfigured()
        val url = URL(projectUrl.trimEnd('/') + path)
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 25_000
            setRequestProperty("apikey", publicKey)
            setRequestProperty("Accept", "application/json")
            if (bearer.isNullOrBlank()) setRequestProperty("Authorization", "Bearer $publicKey")
            else setRequestProperty("Authorization", "Bearer $bearer")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
            }
            if (!prefer.isNullOrBlank()) setRequestProperty("Prefer", prefer)
        }
        try {
            if (body != null) connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val error = runCatching { JSONObject(text) }.getOrDefault(JSONObject())
                val message = error.optString("msg").ifBlank { error.optString("message") }
                    .ifBlank { error.optString("error_description") }
                    .ifBlank { "Request failed ($code)." }
                throw IllegalStateException(message.take(220))
            }
            when {
                text.isBlank() -> JSONObject()
                text.trimStart().startsWith("[") -> JSONObject().put("__array", JSONArray(text))
                text.trimStart().startsWith("{") -> JSONObject(text)
                else -> JSONObject().put("value", text)
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun makeVerifier(): String {
        val bytes = ByteArray(32).also(SecureRandom()::nextBytes)
        return Base64.encodeToString(bytes, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    private fun challenge(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.encodeToString(digest, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
    }

    companion object {
        private val EMAIL_TOKEN_TYPES = setOf("signup", "email", "magiclink", "recovery", "invite", "email_change")
    }
}
