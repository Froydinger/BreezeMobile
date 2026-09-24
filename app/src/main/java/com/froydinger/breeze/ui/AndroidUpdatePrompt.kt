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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.froydinger.breeze.updates.AndroidUpdate

@Composable
fun AndroidUpdatePrompt(update: AndroidUpdate, onDismiss: () -> Unit, onDownload: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(BreezeIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Time to update") },
        text = {
            Column {
                Text("Breeze ${update.versionName} is ready to download.")
                Spacer(Modifier.height(9.dp))
                Text("After it downloads, tap the Android download notification to install the signed update.")
            }
        },
        confirmButton = { TextButton(onClick = onDownload) { Text("Download update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

fun enqueueAndroidUpdateDownload(context: Context, update: AndroidUpdate): Boolean = runCatching {
    val manager = context.getSystemService(DownloadManager::class.java)
        ?: error("Android download service is unavailable")
    val request = DownloadManager.Request(Uri.parse(update.apkUrl))
        .setTitle("Breeze Android update")
        .setDescription("Downloading Breeze ${update.versionName}")
        .setMimeType("application/vnd.android.package-archive")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setAllowedOverMetered(true)
        .setAllowedOverRoaming(false)
        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, update.apkFileName)
    manager.enqueue(request)
}.isSuccess
