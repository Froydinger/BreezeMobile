package com.froydinger.breeze.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

/** Hands FCM callbacks to durable work so Android can finish processing after this service returns. */
class BreezeFirebaseMessagingService : FirebaseMessagingService() {
    override fun onRegistered(installationId: String) {
        ReminderPushWork.enqueueRegistration(applicationContext, installationId)
    }

    override fun onUnregistered(installationId: String) {
        getSharedPreferences("breeze_fcm_registration", MODE_PRIVATE)
            .edit()
            .remove("installation_id")
            .apply()
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val data = message.data
        if (data["type"] != "breeze_reminder") return
        val reminderId = data["reminder_id"]?.takeIf { it.length in 1..128 } ?: return
        val dueAt = data["due_at"]?.toLongOrNull()?.takeIf { it > 0L } ?: return
        ReminderPushWork.enqueueReminderDelivery(applicationContext, reminderId, dueAt)
    }
}
