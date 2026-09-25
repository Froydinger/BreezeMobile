package com.froydinger.breeze.notifications

import android.content.Context
import androidx.compose.runtime.snapshotFlow
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.froydinger.breeze.BreezeApplication
import com.froydinger.breeze.BrowserState
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.TimeUnit

private const val KEY_INSTALLATION_ID = "installation_id"
private const val KEY_REMINDER_ID = "reminder_id"
private const val KEY_DUE_AT = "due_at"

object ReminderPushWork {
    fun enqueueRegistration(context: Context, installationId: String) {
        val work = OneTimeWorkRequestBuilder<ReminderRegistrationWorker>()
            .setInputData(workDataOf(KEY_INSTALLATION_ID to installationId))
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "breeze-reminder-registration",
            ExistingWorkPolicy.REPLACE,
            work,
        )
    }

    fun enqueueReminderDelivery(context: Context, reminderId: String, dueAt: Long) {
        val work = OneTimeWorkRequestBuilder<ReminderDeliveryWorker>()
            .setInputData(workDataOf(KEY_REMINDER_ID to reminderId, KEY_DUE_AT to dueAt))
            .setConstraints(networkConstraints())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "breeze-reminder-delivery:$reminderId:$dueAt",
            ExistingWorkPolicy.KEEP,
            work,
        )
    }

    private fun networkConstraints() = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()
}

class ReminderRegistrationWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val state = awaitBrowserState() ?: return Result.retry()
        val installationId = inputData.getString(KEY_INSTALLATION_ID) ?: return Result.failure()
        val canRegister = withContext(Dispatchers.Main.immediate) {
            state.cloudAccount.signedIn && state.cloudAccount.preferences.reminders &&
                ReminderScheduler.notificationsAllowed(applicationContext)
        }
        if (!canRegister) {
            runCatching { FirebaseMessaging.getInstance().unregister() }
            return Result.success()
        }
        return try {
            withContext(Dispatchers.Main.immediate) {
                ReminderPushRegistration.acceptRegistration(applicationContext, state.cloudAccount, installationId)
            }
            Result.success()
        } catch (_: Exception) {
            withContext(Dispatchers.Main.immediate) {
                state.cloudAccount.syncStatus = "Reminder push will retry when Breeze is online"
            }
            Result.retry()
        }
    }

    private suspend fun awaitBrowserState(): BrowserState? {
        val app = applicationContext as? BreezeApplication ?: return null
        val state = withContext(Dispatchers.Main.immediate) { app.browser }
        val ready = withTimeoutOrNull(20_000L) {
            withContext(Dispatchers.Main.immediate) { snapshotFlow { state.ready }.first { it } }
        } ?: false
        return state.takeIf { ready }
    }
}

class ReminderDeliveryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val reminderId = inputData.getString(KEY_REMINDER_ID) ?: return Result.failure()
        val dueAt = inputData.getLong(KEY_DUE_AT, 0L).takeIf { it > 0L } ?: return Result.failure()
        val app = applicationContext as? BreezeApplication ?: return Result.failure()
        val state = withContext(Dispatchers.Main.immediate) { app.browser }
        val ready = withTimeoutOrNull(20_000L) {
            withContext(Dispatchers.Main.immediate) { snapshotFlow { state.ready }.first { it } }
        } ?: false
        if (!ready) return Result.retry()

        val eligible = withContext(Dispatchers.Main.immediate) {
            state.cloudAccount.signedIn && state.cloudAccount.preferences.reminders &&
                ReminderScheduler.notificationsAllowed(applicationContext)
        }
        if (!eligible) return Result.success()

        var reminder = withContext(Dispatchers.Main.immediate) {
            state.reminders.firstOrNull { it.id == reminderId && it.dueAt == dueAt }
        }
        if (reminder == null) {
            try {
                withContext(Dispatchers.Main.immediate) { state.syncCloudCollection("reminders") }
            } catch (_: Exception) {
                return Result.retry()
            }
            reminder = withContext(Dispatchers.Main.immediate) {
                state.reminders.firstOrNull { it.id == reminderId && it.dueAt == dueAt }
            }
        }
        val dueReminder = reminder ?: return Result.success()
        if (dueReminder.deliveredAt != null || dueReminder.dueAt > System.currentTimeMillis()) return Result.success()

        val posted = withContext(Dispatchers.Main.immediate) {
            ReminderScheduler.post(applicationContext, dueReminder)
        }
        if (!posted) return Result.success()
        withContext(Dispatchers.Main.immediate) {
            if (dueReminder.repeat == ReminderRepeat.NONE) {
                state.completeReminder(reminderId)
            } else {
                val next = ReminderScheduler.nextOccurrence(dueReminder)
                if (next != null) state.advanceReminderOccurrence(reminderId, next)
            }
        }
        return Result.success()
    }
}
