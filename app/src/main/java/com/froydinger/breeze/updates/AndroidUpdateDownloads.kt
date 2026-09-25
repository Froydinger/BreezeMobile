package com.froydinger.breeze.updates

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.froydinger.breeze.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DownloadedAndroidUpdate(val update: AndroidUpdate, val downloadId: Long)

object AndroidUpdateDownloads {
    private const val PREFS = "breeze_update_downloads"
    private const val KEY_ACTIVE_ID = "active_download_id"
    private const val KEY_READY_ID = "ready_download_id"
    private const val KEY_TAG = "tag"
    private const val KEY_VERSION_NAME = "version_name"
    private const val KEY_VERSION_CODE = "version_code"
    private const val KEY_APK_NAME = "apk_name"
    private const val KEY_APK_URL = "apk_url"
    private val suppressedForSession = mutableSetOf<Long>()

    private val _completed = MutableStateFlow<DownloadedAndroidUpdate?>(null)
    val completed: StateFlow<DownloadedAndroidUpdate?> = _completed.asStateFlow()
    private val _failed = MutableStateFlow<AndroidUpdate?>(null)
    val failed: StateFlow<AndroidUpdate?> = _failed.asStateFlow()

    /** Enqueues the verified release asset and remembers its identity for the completion receiver. */
    fun enqueue(context: Context, update: AndroidUpdate): Boolean = runCatching {
        check(AndroidUpdatePolicy.isTrustedReleaseAsset(update.tag, update.versionName, update.apkFileName, update.apkUrl))
        val manager = context.getSystemService(DownloadManager::class.java)
            ?: error("Android download service is unavailable")
        val request = DownloadManager.Request(Uri.parse(update.apkUrl))
            .setTitle("Breeze Android update")
            .setDescription("Downloading. When it’s ready, tap the notification to install.")
            .setMimeType(APK_MIME)
            // Download Manager's completed notification opens its system install flow directly.
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)
            .setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, update.apkFileName)
        val id = manager.enqueue(request)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putLong(KEY_ACTIVE_ID, id)
            .remove(KEY_READY_ID)
            .putString(KEY_TAG, update.tag)
            .putString(KEY_VERSION_NAME, update.versionName)
            .putInt(KEY_VERSION_CODE, update.versionCode)
            .putString(KEY_APK_NAME, update.apkFileName)
            .putString(KEY_APK_URL, update.apkUrl)
            .commit()
        suppressedForSession.clear()
        _completed.value = null
        _failed.value = null
        true
    }.getOrDefault(false)

    fun isDownloading(context: Context, tag: String): Boolean = runCatching {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val id = prefs.getLong(KEY_ACTIVE_ID, -1L)
        val manager = context.getSystemService(DownloadManager::class.java)
        if (id <= 0 || prefs.getString(KEY_TAG, null) != tag || manager == null) false
        else {
            manager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
                if (!cursor.moveToFirst()) false
                else when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_PENDING, DownloadManager.STATUS_RUNNING, DownloadManager.STATUS_PAUSED -> true
                    else -> false
                }
            }
        } == true
    }.getOrDefault(false)

    fun clearFailure(tag: String) {
        if (_failed.value?.tag == tag) _failed.value = null
    }

    fun onDownloadComplete(context: Context, downloadId: Long) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (downloadId <= 0 || prefs.getLong(KEY_ACTIVE_ID, -1L) != downloadId) return
        val update = readUpdate(prefs) ?: return
        val manager = context.getSystemService(DownloadManager::class.java) ?: return
        val statusAndBytes = runCatching {
            manager.query(DownloadManager.Query().setFilterById(downloadId))?.use { cursor ->
                if (!cursor.moveToFirst()) null
                else cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) to
                    cursor.getLong(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
            }
        }.getOrNull() ?: return
        if (statusAndBytes.first != DownloadManager.STATUS_SUCCESSFUL || statusAndBytes.second <= 0L) {
            if (statusAndBytes.first == DownloadManager.STATUS_FAILED || statusAndBytes.first == DownloadManager.STATUS_SUCCESSFUL) {
                prefs.edit().remove(KEY_ACTIVE_ID).remove(KEY_READY_ID).commit()
                AndroidUpdateChecker.undismiss(context, update.tag)
                _failed.value = update
            }
            return
        }

        val ready = DownloadedAndroidUpdate(update, downloadId)
        AndroidUpdateChecker.dismiss(context, update.tag)
        prefs.edit().putLong(KEY_READY_ID, downloadId).commit()
        if (downloadId !in suppressedForSession) _completed.value = ready
    }

    /** Restores a completed APK when Breeze is reopened after the download finished in the background. */
    fun refresh(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val activeId = prefs.getLong(KEY_ACTIVE_ID, -1L)
        if (activeId > 0 && prefs.getLong(KEY_READY_ID, -1L) != activeId) {
            // Reconcile the download status on resume too, in case Android delivered completion while Breeze was stopped.
            onDownloadComplete(context, activeId)
        }
        val ready = runCatching {
            val id = prefs.getLong(KEY_READY_ID, -1L)
            val update = readUpdate(prefs)
            if (id > 0 && update != null && BuildConfig.VERSION_CODE >= update.versionCode) {
                prefs.edit().remove(KEY_READY_ID).remove(KEY_ACTIVE_ID).commit()
                return@runCatching null
            }
            val manager = context.getSystemService(DownloadManager::class.java)
            if (id <= 0 || update == null || manager == null) null
            else {
                val uri = manager.getUriForDownloadedFile(id)
                if (uri == null) {
                    prefs.edit().remove(KEY_READY_ID).remove(KEY_ACTIVE_ID).commit()
                    AndroidUpdateChecker.undismiss(context, update.tag)
                    null
                } else DownloadedAndroidUpdate(update, id)
            }
        }.getOrNull()
        _completed.value = ready?.takeUnless { it.downloadId in suppressedForSession }
    }

    fun dismissPrompt(downloadId: Long) {
        suppressedForSession += downloadId
        _completed.value = null
    }

    /** Opens Android Downloads; its notification/item launches the system installer under Downloads' authority. */
    fun openDownloads(context: Context, downloadId: Long): Boolean = runCatching {
        val ready = _completed.value?.takeIf { it.downloadId == downloadId }
            ?: refreshAndGet(context, downloadId)
        check(ready != null) { "The downloaded update is no longer available" }
        context.startActivity(Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    private fun refreshAndGet(context: Context, id: Long): DownloadedAndroidUpdate? {
        refresh(context)
        return _completed.value?.takeIf { it.downloadId == id }
    }

    private fun readUpdate(prefs: android.content.SharedPreferences): AndroidUpdate? {
        val update = AndroidUpdate(
            tag = prefs.getString(KEY_TAG, null) ?: return null,
            versionName = prefs.getString(KEY_VERSION_NAME, null) ?: return null,
            versionCode = prefs.getInt(KEY_VERSION_CODE, -1),
            apkUrl = prefs.getString(KEY_APK_URL, null) ?: return null,
            apkFileName = prefs.getString(KEY_APK_NAME, null) ?: return null,
        )
        return update.takeIf {
            it.versionCode > 0 && AndroidUpdatePolicy.isTrustedReleaseAsset(it.tag, it.versionName, it.apkFileName, it.apkUrl)
        }
    }

    private const val APK_MIME = "application/vnd.android.package-archive"
}

class AndroidUpdateDownloadReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != DownloadManager.ACTION_DOWNLOAD_COMPLETE) return
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        AndroidUpdateDownloads.onDownloadComplete(context, id)
    }
}
