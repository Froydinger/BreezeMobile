package com.froydinger.breeze.sync

import com.froydinger.breeze.BrowserState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

class CloudSyncCoordinator(
    private val state: BrowserState,
    private val account: CloudAccountRepository,
) {
    private val mutex = Mutex()

    suspend fun syncCollection(collection: String): Int = mutex.withLock {
        require(collection in CloudSyncPreferences.COLLECTIONS)
        check(account.configured) { "Breeze cloud sync is not configured in this build." }
        check(account.signedIn) { "Sign in to your Breeze account first." }
        account.loadPreferences()
        if (!account.preferences.enabled(collection)) return@withLock 0
        account.syncStatus = "Syncing ${collection}…"
        try {
            val remote = account.loadCollection(collection)
            val merged = state.mergeCloudCollection(collection, remote ?: JSONObject())
            account.saveCollection(collection, merged)
            account.syncStatus = "Synced just now"
            1
        } catch (error: Exception) {
            account.syncStatus = "Sync paused until connected"
            throw error
        }
    }

    suspend fun syncNow(): Int = mutex.withLock {
        check(account.configured) { "Breeze cloud sync is not configured in this build." }
        check(account.signedIn) { "Sign in to your Breeze account first." }
        account.syncStatus = "Syncing selected data…"
        try {
            account.loadPreferences()
            var updated = 0
            for (collection in CloudSyncPreferences.COLLECTIONS) {
                if (!account.preferences.enabled(collection)) continue
                val remote = account.loadCollection(collection)
                val merged = state.mergeCloudCollection(collection, remote ?: JSONObject())
                account.saveCollection(collection, merged)
                updated += 1
            }
            account.syncStatus = if (updated == 0) "No sync categories selected" else "Synced just now"
            updated
        } catch (error: Exception) {
            account.syncStatus = "Sync paused until connected"
            throw error
        }
    }
}
