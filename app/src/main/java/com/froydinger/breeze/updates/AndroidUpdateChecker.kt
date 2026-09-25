package com.froydinger.breeze.updates

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class AndroidUpdate(
    val tag: String,
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val apkFileName: String,
)

object AndroidUpdateChecker {
    private const val PREFS = "breeze_update_check"
    private const val MANIFEST_URL =
        "https://raw.githubusercontent.com/Froydinger/BreezeMobile/main/update/latest.json"
    private const val TIMEOUT_MS = 6_000
    private const val MAX_MANIFEST_BYTES = 16_384

    /** Checks the public Git-tracked release manifest at each app launch, using the cached release offline. */
    suspend fun check(context: Context, currentVersionCode: Int = com.froydinger.breeze.BuildConfig.VERSION_CODE): AndroidUpdate? =
        withContext(Dispatchers.IO) {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val cached = readCached(prefs)
            val dismissedTag = prefs.getString("dismissed_tag", null)
            try {
                val connection = (URL(MANIFEST_URL).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    instanceFollowRedirects = false
                    useCaches = false
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Cache-Control", "no-cache")
                    setRequestProperty("User-Agent", "Breeze-Android-Update-Check")
                }
                val manifest = try {
                    check(connection.responseCode == HttpURLConnection.HTTP_OK) { "Update manifest request failed" }
                    val bytes = ByteArrayOutputStream()
                    connection.inputStream.use { input ->
                        val buffer = ByteArray(1024)
                        var total = 0
                        while (true) {
                            val count = input.read(buffer)
                            if (count < 0) break
                            total += count
                            check(total <= MAX_MANIFEST_BYTES) { "Update manifest too large" }
                            bytes.write(buffer, 0, count)
                        }
                    }
                    val body = bytes.toString(Charsets.UTF_8.name())
                    parseManifest(JSONObject(body))
                } finally {
                    connection.disconnect()
                }
                if (manifest != null) writeCached(prefs, manifest)
                eligible(manifest, dismissedTag, currentVersionCode)
            } catch (_: Exception) {
                eligible(cached, dismissedTag, currentVersionCode)
            }
        }

    fun dismiss(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString("dismissed_tag", tag).apply()
    }

    private fun parseManifest(json: JSONObject): AndroidUpdate? {
        val tag = json.optString("tag", "")
        val versionName = json.optString("versionName", "")
        val versionCode = json.optInt("versionCode", -1)
        val apkFileName = json.optString("apkFileName", "")
        val apkUrl = json.optString("apkUrl", "")
        if (versionCode <= 0 || versionName.isBlank()) return null
        if (!AndroidUpdatePolicy.isTrustedReleaseAsset(tag, versionName, apkFileName, apkUrl)) return null
        return AndroidUpdate(tag, versionName, versionCode, apkUrl, apkFileName)
    }

    private fun readCached(prefs: android.content.SharedPreferences): AndroidUpdate? {
        val tag = prefs.getString("tag", null) ?: return null
        val versionName = prefs.getString("version_name", null) ?: return null
        val versionCode = prefs.getInt("version_code", -1)
        val apkFileName = prefs.getString("apk_file_name", null) ?: return null
        val apkUrl = prefs.getString("apk_url", null) ?: return null
        if (!AndroidUpdatePolicy.isTrustedReleaseAsset(tag, versionName, apkFileName, apkUrl)) return null
        return AndroidUpdate(tag, versionName, versionCode, apkUrl, apkFileName)
    }

    private fun writeCached(prefs: android.content.SharedPreferences, update: AndroidUpdate) {
        prefs.edit()
            .putString("tag", update.tag)
            .putString("version_name", update.versionName)
            .putInt("version_code", update.versionCode)
            .putString("apk_file_name", update.apkFileName)
            .putString("apk_url", update.apkUrl)
            .apply()
    }

    private fun eligible(update: AndroidUpdate?, dismissedTag: String?, currentVersionCode: Int): AndroidUpdate? =
        update?.takeIf { AndroidUpdatePolicy.shouldOffer(it.versionCode, currentVersionCode) && it.tag != dismissedTag }
}

internal object AndroidUpdatePolicy {
    private val tagPattern = Regex("^android-v\\d+\\.\\d+\\.\\d+(?:-beta\\.\\d+)?$")

    fun shouldOffer(remoteVersionCode: Int, installedVersionCode: Int): Boolean =
        remoteVersionCode > installedVersionCode

    fun isTrustedReleaseAsset(tag: String, versionName: String, fileName: String, url: String): Boolean {
        if (!tagPattern.matches(tag)) return false
        val releaseVersion = tag.removePrefix("android-v").substringBefore("-beta.")
        val expectedFile = "Breeze-Android-${tag.removePrefix("android-v")}-arm64.apk"
        val expectedUrl = "https://github.com/Froydinger/BreezeMobile/releases/download/$tag/$expectedFile"
        return versionName == releaseVersion && fileName == expectedFile && url == expectedUrl
    }
}
