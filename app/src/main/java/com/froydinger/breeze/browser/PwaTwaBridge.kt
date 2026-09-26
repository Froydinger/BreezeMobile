package com.froydinger.breeze.browser

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import androidx.browser.customtabs.CustomTabsService
import androidx.browser.customtabs.CustomTabsSessionToken
import androidx.browser.customtabs.TrustedWebUtils
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

/**
 * Gives Android Web Apps installed through this Breeze package a first-party standalone launch
 * path. The system installer callback is the source of truth for the package and the manifest
 * that were installed; this avoids depending on package-manager metadata visibility for generated
 * Web App packages.
 */
internal object PwaTwaBridge {
    private const val SESSION_LIFETIME_MS = 5 * 60 * 1000L
    private const val INSTALL_RECORDS = "breeze_pwa_install_records"
    private const val MANIFEST_SUFFIX = ".manifest"
    private const val START_SUFFIX = ".start"

    private data class SessionGrant(
        val packageName: String,
        val origin: String,
        val expiresAt: Long,
    )

    private data class InstalledWebApp(
        val manifestUrl: String,
        val startUrl: String,
        val origin: String,
    )

    private val sessionGrants = ConcurrentHashMap<CustomTabsSessionToken, SessionGrant>()

    /** Persisted only after Android reports a successful WebAppManager installation. */
    fun recordInstalledWebApp(
        context: Context,
        packageName: String,
        manifestUrl: String,
        startUrl: String,
    ): Boolean {
        if (packageName.isBlank() || packageName == context.packageName) return false
        val manifest = canonicalSecureUrl(manifestUrl) ?: return false
        val start = canonicalSecureUrl(startUrl) ?: return false
        val origin = secureOrigin(manifest) ?: return false
        if (origin != secureOrigin(start)) return false
        return context.getSharedPreferences(INSTALL_RECORDS, Context.MODE_PRIVATE)
            .edit()
            .putString(packageName + MANIFEST_SUFFIX, manifest)
            .putString(packageName + START_SUFFIX, start)
            .commit()
    }

    fun removeInstalledWebApp(context: Context, packageName: String) {
        if (packageName.isBlank()) return
        context.getSharedPreferences(INSTALL_RECORDS, Context.MODE_PRIVATE)
            .edit()
            .remove(packageName + MANIFEST_SUFFIX)
            .remove(packageName + START_SUFFIX)
            .apply()
    }

    fun recordSession(context: Context, token: CustomTabsSessionToken, callerUid: Int) {
        val now = SystemClock.elapsedRealtime()
        sessionGrants.entries.removeAll { it.value.expiresAt <= now }
        val packages = context.packageManager.getPackagesForUid(callerUid).orEmpty()
        val trustedPackage = packages.firstNotNullOfOrNull { packageName ->
            installedWebAppFromThisBrowser(context, packageName)?.let { packageName to it.origin }
        }
        if (trustedPackage == null) {
            sessionGrants.remove(token)
        } else {
            sessionGrants[token] = SessionGrant(
                packageName = trustedPackage.first,
                origin = trustedPackage.second,
                expiresAt = now + SESSION_LIFETIME_MS,
            )
        }
    }

    fun validateRelationship(
        context: Context,
        token: CustomTabsSessionToken,
        callerUid: Int,
        relationship: Int,
        requestedOrigin: Uri,
    ): Boolean {
        if (relationship != CustomTabsService.RELATION_HANDLE_ALL_URLS) return false
        val grant = sessionGrants[token] ?: return false
        if (grant.expiresAt <= SystemClock.elapsedRealtime()) {
            sessionGrants.remove(token)
            return false
        }
        if (context.packageManager.getPackagesForUid(callerUid).orEmpty().none { it == grant.packageName }) return false
        val currentOrigin = installedWebAppFromThisBrowser(context, grant.packageName)?.origin ?: return false
        return currentOrigin == grant.origin && secureOrigin(requestedOrigin.toString()) == grant.origin
    }

    fun consumeTrustedLaunch(context: Context, intent: Intent): Boolean {
        if (!intent.getBooleanExtra(TrustedWebUtils.EXTRA_LAUNCH_AS_TRUSTED_WEB_ACTIVITY, false)) return false
        val token = CustomTabsSessionToken.getSessionTokenFromIntent(intent) ?: return false
        val grant = sessionGrants.remove(token) ?: return false
        if (grant.expiresAt <= SystemClock.elapsedRealtime()) return false

        val requestedOrigin = secureOrigin(intent.dataString.orEmpty()) ?: return false
        val currentOrigin = installedWebAppFromThisBrowser(context, grant.packageName)?.origin ?: return false
        return currentOrigin == grant.origin && requestedOrigin == grant.origin
    }

    fun isInstalledByThisBrowser(
        context: Context,
        packageName: String,
        expectedManifestUrl: String,
        expectedStartUrl: String,
    ): Boolean {
        val installed = installedWebAppFromThisBrowser(context, packageName) ?: return false
        return installed.manifestUrl == canonicalSecureUrl(expectedManifestUrl) &&
            installed.startUrl == canonicalSecureUrl(expectedStartUrl)
    }

    private fun installedWebAppFromThisBrowser(context: Context, packageName: String): InstalledWebApp? {
        if (packageName.isBlank()) return null
        val preferences = context.getSharedPreferences(INSTALL_RECORDS, Context.MODE_PRIVATE)
        val manifestUrl = canonicalSecureUrl(preferences.getString(packageName + MANIFEST_SUFFIX, null).orEmpty()) ?: return null
        val startUrl = canonicalSecureUrl(preferences.getString(packageName + START_SUFFIX, null).orEmpty()) ?: return null
        val manifestOrigin = secureOrigin(manifestUrl) ?: return null
        if (manifestOrigin != secureOrigin(startUrl)) return null
        return InstalledWebApp(manifestUrl = manifestUrl, startUrl = startUrl, origin = manifestOrigin)
    }

    private fun canonicalSecureUrl(rawUrl: String): String? = runCatching {
        val uri = URI(rawUrl).normalize()
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() || uri.rawUserInfo != null) return null
        val host = uri.host.lowercase()
        val port = uri.port
        val authority = if (port == -1 || port == 443) host else "$host:$port"
        val path = uri.rawPath.orEmpty().ifBlank { "/" }
        val query = uri.rawQuery?.let { "?$it" }.orEmpty()
        "https://$authority$path$query"
    }.getOrNull()

    private fun secureOrigin(rawUrl: String): String? = runCatching {
        val canonical = URI(canonicalSecureUrl(rawUrl) ?: return null)
        val host = canonical.host.lowercase()
        val port = canonical.port
        if (port == -1 || port == 443) "https://$host" else "https://$host:$port"
    }.getOrNull()
}
