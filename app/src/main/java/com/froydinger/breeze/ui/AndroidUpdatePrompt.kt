package com.froydinger.breeze.ui

import android.content.Context
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
import com.froydinger.breeze.updates.AndroidUpdateDownloads
import com.froydinger.breeze.updates.DownloadedAndroidUpdate

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
                Text("Breeze will let you know when it’s ready. Android will ask you to confirm the installation.")
            }
        },
        confirmButton = { TextButton(onClick = onDownload) { Text("Download update") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

@Composable
fun AndroidUpdateReadyPrompt(
    ready: DownloadedAndroidUpdate,
    onDismiss: () -> Unit,
    onOpenDownloads: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(BreezeIcons.Download, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Update downloaded") },
        text = {
            Column {
                Text("Breeze ${ready.update.versionName} is ready to install.")
                Spacer(Modifier.height(9.dp))
                Text("Tap the Android download notification to install directly, or open Downloads and tap the APK. Android will ask you to confirm.")
            }
        },
        confirmButton = { TextButton(onClick = onOpenDownloads) { Text("Open Downloads") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Later") } },
    )
}

fun enqueueAndroidUpdateDownload(context: Context, update: AndroidUpdate): Boolean =
    AndroidUpdateDownloads.enqueue(context, update)
