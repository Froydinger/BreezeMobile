package com.froydinger.breeze.notifications

import android.content.Context
import com.froydinger.breeze.sync.CloudAccountRepository
import com.google.firebase.messaging.FirebaseMessaging

/** FCM is registered only after the user enables reminder sync and Android allows notifications. */
object ReminderPushRegistration {
    private const val PREFS = "breeze_fcm_registration"
    private const val INSTALLATION_ID = "installation_id"

    fun registerIfAllowed(context: Context, account: CloudAccountRepository): Boolean {
        if (!account.signedIn || !account.preferences.reminders || !ReminderScheduler.notificationsAllowed(context)) return false
        runCatching { FirebaseMessaging.getInstance().register() }
            .onFailure { account.syncStatus = "Reminder push is waiting for Google Play services" }
        return true
    }

    suspend fun acceptRegistration(context: Context, account: CloudAccountRepository, installationId: String) {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (account.signedIn && account.preferences.reminders && ReminderScheduler.notificationsAllowed(context)) {
            stored.edit().putString(INSTALLATION_ID, installationId).apply()
            account.setPushDevice(installationId, true)
        } else {
            stored.edit().remove(INSTALLATION_ID).apply()
            runCatching { FirebaseMessaging.getInstance().unregister() }
        }
    }

    suspend fun disable(context: Context, account: CloudAccountRepository) {
        val stored = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val installationId = stored.getString(INSTALLATION_ID, null)
        if (!installationId.isNullOrBlank() && account.signedIn) {
            runCatching { account.setPushDevice(installationId, false) }
        }
        stored.edit().remove(INSTALLATION_ID).apply()
        runCatching { FirebaseMessaging.getInstance().unregister() }
    }
}
