package com.froydinger.breeze.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal data class NativeAppDestination(val name: String, val component: ComponentName)

/** Offer only installed handlers for this exact web URL, excluding general-purpose browsers. */
@Suppress("DEPRECATION")
internal fun nativeAppFor(context: Context, uri: Uri): NativeAppDestination? {
    val manager = context.packageManager
    val flags = PackageManager.MATCH_DEFAULT_ONLY
    val mainPackage = context.packageName.removeSuffix(".dev")
    val breezePackages = setOf(mainPackage, "$mainPackage.dev")
    fun link(target: Uri): Intent {
        // Android intent matching distinguishes an empty path from the site's root path.
        // Normalize bare origins so `https://chatgpt.com` matches the same app link as `/`.
        val normalized = if (target.scheme in setOf("http", "https") && target.path.isNullOrEmpty()) {
            target.buildUpon().encodedPath("/").build()
        } else target
        return Intent(Intent.ACTION_VIEW, normalized).addCategory(Intent.CATEGORY_BROWSABLE)
    }
    val browsers = manager.queryIntentActivities(link(Uri.parse("https://example.com/")), flags)
        .map { it.activityInfo.packageName }.toSet()
    return manager.queryIntentActivities(link(uri), flags).asSequence()
        .filter { it.activityInfo.exported && it.activityInfo.enabled }
        .filter { it.activityInfo.packageName !in breezePackages && it.activityInfo.packageName !in browsers }
        .sortedByDescending { it.priority }
        .firstOrNull()?.let { NativeAppDestination(it.loadLabel(manager).toString(), ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
}

@Composable
fun OpenInAppBanner(
    url: String,
    modifier: Modifier = Modifier,
    suppress: Boolean = false,
    onOpeningApp: (String) -> Unit = {},
    onOpenFailed: (String) -> Unit = {},
    onNotice: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val uri = remember(url) { runCatching { Uri.parse(url) }.getOrNull() }
    val host = uri?.host?.lowercase() ?: return
    if (uri.scheme !in listOf("https", "http")) return
    if (suppress) return
    var dismissed by rememberSaveable(host) { mutableStateOf(false) }
    var destination by remember(url) { mutableStateOf<NativeAppDestination?>(null) }
    LaunchedEffect(url) {
        destination = withContext(Dispatchers.IO) { runCatching { nativeAppFor(context, uri) }.getOrNull() }
    }
    val app = destination ?: return
    if (dismissed) return
    Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), tonalElevation = 4.dp, shadowElevation = 3.dp) {
        Row(Modifier.padding(start = 14.dp, top = 4.dp, bottom = 4.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Open in ${app.name}", style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(host.removePrefix("www."), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE).setComponent(app.component)
                dismissed = true
                onOpeningApp(url)
                runCatching { context.startActivity(intent) }
                    .onFailure {
                        dismissed = false
                        onOpenFailed(url)
                        onNotice("Couldn’t open ${app.name}. You can keep browsing this page in Breeze.")
                    }
            }) { Text("Open") }
            IconButton(onClick = { dismissed = true }) { Icon(BreezeIcons.Close, contentDescription = "Dismiss open in app") }
        }
    }
}
