package com.froydinger.breeze.ui

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private const val TEST_UPDATE_URL =
    "https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.7-beta.1/Breeze-Android-0.1.7-beta.1-arm64.apk"
private const val TEST_UPDATE_FILE = "Breeze-Android-0.1.7-beta.1-arm64.apk"

@Composable
fun DebugUpdatePrompt(onDismiss: () -> Unit, onDownload: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(BreezeIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Time to update") },
        text = {
            Column {
                Text("Breeze 0.1.7 beta is ready to download.")
                Spacer(androidx.compose.ui.Modifier.height(9.dp))
                Text("After it downloads, tap the Android download notification to install the signed update.")
                Spacer(androidx.compose.ui.Modifier.height(9.dp))
                Text("This update prompt is a Breeze Dev test.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDownload) { Text("Download update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

fun enqueueTestUpdateDownload(context: Context): Boolean = runCatching {
    val manager = context.getSystemService(DownloadManager::class.java)
        ?: error("Android download service is unavailable")
    val request = DownloadManager.Request(Uri.parse(TEST_UPDATE_URL))
        .setTitle("Breeze Android update")
        .setDescription("Downloading Breeze 0.1.7 beta")
        .setMimeType("application/vnd.android.package-archive")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, TEST_UPDATE_FILE)
    manager.enqueue(request)
}.isSuccess
