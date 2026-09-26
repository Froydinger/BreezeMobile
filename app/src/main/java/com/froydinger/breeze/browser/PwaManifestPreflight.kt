package com.froydinger.breeze.browser

import android.graphics.BitmapFactory
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.nio.charset.StandardCharsets

/** Details verified from the live manifest before Android's system PWA installer is called. */
internal data class PwaInstallMetadata(
    val title: String,
    val displayMode: String,
    val iconUrl: String,
    val manifestUrl: String,
    val startUrl: String,
    val scopeUrl: String,
)

internal class PwaInstallPreflightException(message: String) : Exception(message)

/**
 * Fetches the linked manifest and launcher icon without using an HTTP cache. This is only a
 * preflight: Android's WebAppManager remains responsible for creating the installed Web App.
 */
internal object PwaManifestPreflight {
    private const val MAX_MANIFEST_BYTES = 512 * 1024
    private const val MAX_ICON_BYTES = 4 * 1024 * 1024

    fun inspect(manifestUrl: String, pageUrl: String, fallbackTitle: String): PwaInstallMetadata {
        val manifestUri = secureHttpUri(manifestUrl, "The page's app manifest URL is invalid.")
        val pageUri = secureHttpUri(pageUrl, "Open a secure web page before installing its app.")
        val (manifestBytes, manifestContentType, finalManifestUrl) = fetch(manifestUri.toString(), MAX_MANIFEST_BYTES)
        if (!hasSupportedManifestMimeType(manifestContentType)) {
            throw PwaInstallPreflightException(
                "The site's manifest is served as ${manifestContentType.ifBlank { "an unknown file type" }}. " +
                    "It must be application/manifest+json or application/json."
            )
        }

        val manifest = runCatching { JSONObject(String(manifestBytes, StandardCharsets.UTF_8)) }
            .getOrElse { throw PwaInstallPreflightException("The site's app manifest is not valid JSON.") }
        val displayOverride = manifest.optJSONArray("display_override")?.let { values ->
            (0 until values.length()).mapNotNull { index -> values.optString(index).takeIf(String::isNotBlank) }
        }.orEmpty()
        val display = standaloneDisplayMode(manifest.optString("display", "browser"), displayOverride)
            ?: throw PwaInstallPreflightException(
                "The site's manifest does not request a standalone or fullscreen app window. " +
                    "Breeze stopped so it won't install as a browser-style shortcut."
            )

        val startUrl = resolveWithinOrigin(finalManifestUrl, manifest.optString("start_url").ifBlank { pageUri.toString() }, pageUri)
        val scopeText = manifest.optString("scope").takeIf(String::isNotBlank)
        val scope = if (scopeText != null) {
            runCatching { URI(finalManifestUrl).resolve(scopeText) }.getOrNull()
                ?: throw PwaInstallPreflightException("The site's app scope is invalid.")
        } else {
            startUrl.resolve(".")
        }
        if (scope.rawQuery != null || scope.rawFragment != null ||
            !sameOrigin(scope, pageUri) || !isWithinWebAppScope(startUrl.path.orEmpty(), scope.path.orEmpty())
        ) {
            throw PwaInstallPreflightException("The site's start page falls outside its app scope.")
        }

        val shortName = manifest.optString("short_name").trim()
        val name = manifest.optString("name").trim()
        val title = (shortName.ifBlank { name }.ifBlank { fallbackTitle.trim() })
            .ifBlank { pageUri.host.orEmpty() }

        val icons = manifest.optJSONArray("icons")
            ?: throw PwaInstallPreflightException("The site's app manifest has no launcher icons.")
        val iconUrls = (0 until icons.length()).mapNotNull { index ->
            val icon = icons.optJSONObject(index) ?: return@mapNotNull null
            val src = icon.optString("src").takeIf(String::isNotBlank) ?: return@mapNotNull null
            val sizes = icon.optString("sizes")
            val purpose = icon.optString("purpose", "any")
            if (declares192Icon(sizes, purpose)) runCatching { URI(finalManifestUrl).resolve(src).toString() }.getOrNull()
            else null
        }
        if (iconUrls.isEmpty()) {
            throw PwaInstallPreflightException("The site's app manifest does not declare a usable 192×192 launcher icon.")
        }
        var verifiedIcon: String? = null
        var iconFailure: Exception? = null
        for (candidate in iconUrls) {
            try {
                verify192Icon(candidate)
                verifiedIcon = candidate
                break
            } catch (error: Exception) {
                iconFailure = error
            }
        }
        val resolvedIcon = verifiedIcon
            ?: throw (iconFailure as? PwaInstallPreflightException
                ?: PwaInstallPreflightException("The site's 192×192 launcher icon could not be loaded or decoded."))

        return PwaInstallMetadata(
            title = title,
            displayMode = display,
            iconUrl = resolvedIcon,
            manifestUrl = finalManifestUrl,
            startUrl = startUrl.toString(),
            scopeUrl = scope.toString(),
        )
    }

    private fun secureHttpUri(raw: String, message: String): URI = runCatching {
        URI(raw).takeIf { it.scheme.equals("https", ignoreCase = true) && !it.host.isNullOrBlank() }
    }.getOrNull() ?: throw PwaInstallPreflightException(message)

    private fun resolveWithinOrigin(manifestUrl: String, value: String, page: URI): URI {
        val resolved = runCatching { URI(manifestUrl).resolve(value) }.getOrNull()
            ?: throw PwaInstallPreflightException("The site's app start URL is invalid.")
        if (!sameOrigin(resolved, page)) {
            throw PwaInstallPreflightException("The site's app start URL points outside the current secure site.")
        }
        return resolved
    }

    private fun sameOrigin(left: URI, right: URI): Boolean {
        fun effectivePort(uri: URI) = if (uri.port >= 0) uri.port else if (uri.scheme.equals("https", true)) 443 else 80
        return left.scheme.equals(right.scheme, true) && left.host.equals(right.host, true) && effectivePort(left) == effectivePort(right)
    }

    private fun verify192Icon(iconUrl: String) {
        val iconUri = secureHttpUri(iconUrl, "The site's launcher icon URL is not secure.")
        val (bytes, contentType, finalUrl) = fetch(iconUri.toString(), MAX_ICON_BYTES)
        secureHttpUri(finalUrl, "The site's launcher icon redirected to an insecure URL.")
        val mime = contentType.substringBefore(';').trim().lowercase()
        if (mime == "image/svg+xml") {
            val svgStart = String(bytes, 0, minOf(bytes.size, 4096), StandardCharsets.UTF_8)
            if (!Regex("<svg(?:\\s|>)", RegexOption.IGNORE_CASE).containsMatchIn(svgStart)) {
                throw PwaInstallPreflightException("The site's 192×192 launcher icon is not a valid SVG image.")
            }
            return
        }
        if (!mime.startsWith("image/") && mime != "application/octet-stream") {
            throw PwaInstallPreflightException("The site's 192×192 launcher icon is not served as a supported image.")
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth < 192 || bounds.outHeight < 192) {
            throw PwaInstallPreflightException("The site's declared 192×192 launcher icon is smaller than 192 pixels.")
        }
        var sampleSize = 1
        while (maxOf(bounds.outWidth / (sampleSize * 2), bounds.outHeight / (sampleSize * 2)) > 512 &&
            minOf(bounds.outWidth / (sampleSize * 2), bounds.outHeight / (sampleSize * 2)) >= 192
        ) {
            sampleSize *= 2
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
        })
        if (bitmap == null || bitmap.width < 192 || bitmap.height < 192) {
            bitmap?.recycle()
            throw PwaInstallPreflightException("The site's declared 192×192 launcher icon could not be decoded at its advertised size.")
        }
        bitmap.recycle()
    }

    private fun fetch(rawUrl: String, maxBytes: Int): Triple<ByteArray, String, String> {
        val uri = secureHttpUri(rawUrl, "A manifest resource used an insecure URL.")
        val connection = (URL(uri.toString()).openConnection() as? HttpURLConnection)
            ?: throw PwaInstallPreflightException("The site's app manifest could not be reached.")
        try {
            connection.instanceFollowRedirects = true
            connection.connectTimeout = 12_000
            connection.readTimeout = 12_000
            connection.useCaches = false
            connection.setRequestProperty("Accept", "application/manifest+json, application/json, image/png, image/jpeg, image/webp, */*;q=0.1")
            connection.setRequestProperty("Cache-Control", "no-cache, no-store, max-age=0")
            connection.setRequestProperty("Pragma", "no-cache")
            val status = connection.responseCode
            if (status !in 200..299) throw PwaInstallPreflightException("The site's manifest or icon returned HTTP $status.")
            val finalUrl = connection.url.toString()
            secureHttpUri(finalUrl, "A manifest resource redirected to an insecure URL.")
            val declaredLength = connection.getHeaderFieldLong("Content-Length", -1L)
            if (declaredLength > maxBytes) throw PwaInstallPreflightException("The site's app manifest or icon is too large to inspect safely.")
            val output = ByteArrayOutputStream(minOf(maxBytes, declaredLength.takeIf { it > 0 }?.toInt() ?: 16 * 1024))
            connection.inputStream.use { input ->
                val buffer = ByteArray(8 * 1024)
                var total = 0
                while (true) {
                    val read = input.read(buffer)
                    if (read < 0) break
                    total += read
                    if (total > maxBytes) throw PwaInstallPreflightException("The site's app manifest or icon is too large to inspect safely.")
                    output.write(buffer, 0, read)
                }
            }
            return Triple(output.toByteArray(), connection.contentType.orEmpty(), finalUrl)
        } finally {
            connection.disconnect()
        }
    }
}

internal fun hasSupportedManifestMimeType(contentType: String): Boolean =
    contentType.substringBefore(';').trim().lowercase() in setOf("application/manifest+json", "application/json")

internal fun standaloneDisplayMode(display: String, overrides: List<String>): String? {
    val allowedModes = setOf("browser", "fullscreen", "standalone", "minimal-ui")
    val selected = overrides.firstOrNull { it.lowercase() in allowedModes }?.lowercase() ?: display.lowercase().ifBlank { "browser" }
    return selected.takeIf { it == "standalone" || it == "fullscreen" }
}

internal fun declares192Icon(sizes: String, purpose: String): Boolean {
    val allowedPurpose = purpose.isBlank() || purpose.split(Regex("\\s+")).any { it.equals("any", true) || it.equals("maskable", true) }
    return allowedPurpose && sizes.split(Regex("\\s+")).any { it.equals("192x192", true) }
}

internal fun isWithinWebAppScope(startPath: String, scopePath: String): Boolean {
    val normalizedStart = startPath.ifBlank { "/" }
    val normalizedScope = scopePath.ifBlank { "/" }
    if (!normalizedStart.startsWith(normalizedScope)) return false
    return normalizedScope.endsWith("/") || normalizedStart.length == normalizedScope.length ||
        normalizedStart.getOrNull(normalizedScope.length) == '/'
}
