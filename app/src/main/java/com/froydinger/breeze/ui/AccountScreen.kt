package com.froydinger.breeze.ui

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.froydinger.breeze.BrowserState
import com.froydinger.breeze.sync.CloudPasskey
import kotlinx.coroutines.launch

@Composable
fun AccountScreen(state: BrowserState) {
    val context = LocalContext.current
    val account = state.cloudAccount
    val scope = rememberCoroutineScope()
    var address by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var showSignOut by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }
    var pendingExport by remember { mutableStateOf<String?>(null) }
    var passkeys by remember { mutableStateOf<List<CloudPasskey>>(emptyList()) }
    var removingPasskey by remember { mutableStateOf<CloudPasskey?>(null) }
    val changeSync: (String, Boolean) -> Unit = { collection, enabled ->
        busy = true
        message = null
        scope.launch {
            runCatching { state.setCloudSyncEnabled(collection, enabled) }
                .onSuccess { message = if (enabled) "${collection.replaceFirstChar(Char::uppercase)} sync is on." else "${collection.replaceFirstChar(Char::uppercase)} sync is off. The cloud copy remains in your account." }
                .onFailure { message = it.message ?: "The sync setting could not be saved." }
            busy = false
        }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val payload = pendingExport
        pendingExport = null
        if (uri != null && payload != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write(payload) }
                ?: error("The selected file could not be opened.")
        }.onFailure { message = it.message ?: "The export could not be saved." }
    }

    LaunchedEffect(account.signedIn, account.email) {
        if (account.signedIn) {
            runCatching { account.loadPreferences() }
                .onFailure { account.syncStatus = "Sign in is active; sync will resume when connected" }
            passkeys = runCatching { account.loadPasskeys() }.getOrDefault(emptyList())
        } else passkeys = emptyList()
    }

    Column(Modifier.fillMaxSize().then(screenEntrance())) {
        Row(Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { state.screen = "settings" }) { Icon(BreezeIcons.ArrowBack, "Back") }
            Text("Breeze account", Modifier.weight(1f).padding(start = 8.dp), style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (!account.signedIn) {
                item {
                    Text("Sign in to back up the things you choose and continue on another Android device. Each sync category starts off.",
                        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = address,
                            onValueChange = { address = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Email") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        )
                        Text(
                            "By submitting the email form or signing in with Google or a passkey, you agree to Breeze’s Terms of Service and Privacy Policy.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                            TextButton(onClick = { state.openLegalDocument("terms-of-service") }) { Text("Terms of service") }
                            TextButton(onClick = { state.openLegalDocument("privacy-policy") }) { Text("Privacy policy") }
                        }
                        Button(
                            onClick = {
                                busy = true; message = null
                                scope.launch {
                                    runCatching { account.signIn(address, password); state.syncCloudNow() }
                                        .onSuccess { message = "Signed in. Your selected data is syncing." }
                                        .onFailure { message = it.message ?: "Could not sign in." }
                                    busy = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = account.configured && !busy && address.isNotBlank() && password.isNotBlank(),
                        ) { Text("Sign in") }
                        OutlinedButton(
                            onClick = {
                                busy = true; message = null
                                scope.launch {
                                    runCatching { account.signUp(address, password) }
                                        .onSuccess { message = it }
                                        .onFailure { message = it.message ?: "Could not create the account." }
                                    busy = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = account.configured && !busy && address.isNotBlank() && password.length >= 8,
                        ) { Text("Create account") }
                        OutlinedButton(
                            onClick = {
                                val activity = context as? Activity
                                if (activity == null) message = "Passkey sign-in could not open from this screen."
                                else {
                                    busy = true; message = null
                                    scope.launch {
                                        runCatching { account.signInWithPasskey(activity) }
                                            .onSuccess {
                                                message = "Signed in with a passkey."
                                                runCatching { state.syncCloudNow() }
                                            }
                                            .onFailure { message = it.message ?: "Passkey sign-in could not finish." }
                                        busy = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = account.configured && !busy,
                        ) { Text("Use a passkey") }
                        OutlinedButton(
                            onClick = {
                                val activity = context as? Activity
                                if (activity == null) message = "Google sign-in could not open from this screen."
                                else runCatching { account.beginGoogleSignIn(activity) }
                                    .onFailure { message = it.message ?: "Google sign-in could not start." }
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = account.configured && !busy,
                        ) { Text("Continue with Google") }
                        if (!account.configured) Text("Account service setup is not complete in this build yet.", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
                item { PrivacyNote("Your account password is used only to sign in. Sync categories start off; your encrypted website password vault always stays on this device.") }
            } else {
                item {
                    AccountStatusCard(
                        email = account.email.orEmpty(),
                        status = account.syncStatus,
                    )
                }
                item { Text("Choose what syncs", style = MaterialTheme.typography.titleMedium) }
                item {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp))) {
                        SyncToggle("Bookmarks", "Saved pages", account.preferences.bookmarks, busy) { changeSync("bookmarks", it) }
                        DividerLine()
                        SyncToggle("Open tabs", "URLs and page titles; pages reopen from the network", account.preferences.tabs, busy) { changeSync("tabs", it) }
                        DividerLine()
                        SyncToggle("History", "Web pages you visited", account.preferences.history, busy) { changeSync("history", it) }
                        DividerLine()
                        SyncToggle("Nav chats", "Messages and source links; attached images stay on this device", account.preferences.chats, busy) { changeSync("chats", it) }
                        DividerLine()
                        SyncToggle("Reminders", "Reminder text and schedule", account.preferences.reminders, busy) { changeSync("reminders", it) }
                    }
                }
                item { PrivacyNote("Passwords and browsing cookies stay on this device. Sync uses an encrypted HTTPS connection and is protected by your account. Turn a category off to stop future syncing; its cloud copy remains until you delete the account.") }
                item {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("Passkeys", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Use your device fingerprint, face, or screen lock to sign in. Passkey private keys stay with your credential provider.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = {
                                val activity = context as? Activity
                                if (activity == null) message = "Passkey setup could not open from this screen."
                                else {
                                    busy = true; message = null
                                    scope.launch {
                                        runCatching { account.registerPasskey(activity) }
                                            .onSuccess {
                                                passkeys = runCatching { account.loadPasskeys() }.getOrDefault(passkeys)
                                                message = "Passkey added to your Breeze account."
                                            }
                                            .onFailure { message = it.message ?: "Could not add a passkey." }
                                        busy = false
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !busy,
                        ) { Text("Add a passkey") }
                        if (passkeys.isEmpty()) {
                            Text("No Breeze account passkeys saved yet.", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            passkeys.forEach { passkey ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Column(Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(passkey.friendlyName, style = MaterialTheme.typography.bodyLarge)
                                        if (passkey.createdAt.isNotBlank()) Text(passkey.createdAt.take(10),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    TextButton(onClick = { removingPasskey = passkey }, enabled = !busy) { Text("Remove") }
                                }
                            }
                        }
                    }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            busy = true
                            scope.launch {
                                runCatching { state.syncCloudNow() }
                                    .onSuccess { message = if (it == 0) "Choose at least one category to sync." else "Your selected data is up to date." }
                                    .onFailure { message = it.message ?: "Sync could not finish." }
                                busy = false
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    ) { if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Icon(BreezeIcons.Refresh, null); Spacer(Modifier.width(8.dp)); Text("Sync now") }
                }
                item {
                    OutlinedButton(
                        onClick = {
                            busy = true
                            scope.launch {
                                runCatching { state.syncCloudNow() }
                                pendingExport = state.exportAccountData().toString(2)
                                exportLauncher.launch("breeze-data-export.json")
                                busy = false
                            }
                        }, modifier = Modifier.fillMaxWidth(), enabled = !busy,
                    ) { Text("Export my data") }
                }
                item {
                    OutlinedButton(onClick = { showSignOut = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy) { Text("Sign out") }
                    TextButton(onClick = { showDelete = true }, modifier = Modifier.fillMaxWidth(), enabled = !busy) {
                        Text("Delete account", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            if (!message.isNullOrBlank()) item { Text(message!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (busy) item { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) { CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp) } }
        }
    }

    if (showSignOut) AlertDialog(
        onDismissRequest = { showSignOut = false },
        title = { Text("Sign out of Breeze?") },
        text = { Text("Synced bookmarks, tabs, history, chats, and reminders will be removed from this device. The cloud copy stays in your account. Your local password vault is kept.") },
        confirmButton = { TextButton(onClick = {
            showSignOut = false; busy = true
            scope.launch {
                runCatching { state.signOutCloud() }
                state.screen = "settings"
                busy = false
            }
        }) { Text("Sign out and clear this device") } },
        dismissButton = { TextButton(onClick = { showSignOut = false }) { Text("Cancel") } },
    )

    if (showDelete) AlertDialog(
        onDismissRequest = { showDelete = false },
        title = { Text("Delete your Breeze account?") },
        text = { Text("This permanently deletes the account and its synced bookmarks, tabs, history, chats, and reminders. It cannot be undone. Your local password vault is kept on this device.") },
        confirmButton = { TextButton(onClick = {
            showDelete = false; busy = true
            scope.launch {
                runCatching { state.deleteCloudAccount() }
                    .onSuccess { state.screen = "settings" }
                    .onFailure { message = it.message ?: "The account could not be deleted." }
                busy = false
            }
        }) { Text("Delete account", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = { showDelete = false }) { Text("Cancel") } },
    )

    removingPasskey?.let { passkey ->
        AlertDialog(
            onDismissRequest = { removingPasskey = null },
            title = { Text("Remove this passkey?") },
            text = { Text("${passkey.friendlyName} will no longer sign in to this Breeze account. You can add a new passkey later.") },
            confirmButton = {
                TextButton(onClick = {
                    removingPasskey = null
                    busy = true
                    scope.launch {
                        runCatching {
                            account.deletePasskey(passkey.id)
                            account.loadPasskeys()
                        }
                            .onSuccess { passkeys = it; message = "Passkey removed." }
                            .onFailure { message = it.message ?: "Could not remove the passkey." }
                        busy = false
                    }
                }) { Text("Remove", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { removingPasskey = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun AccountStatusCard(email: String, status: String) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.surface)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)).padding(16.dp)) {
        Text(email, style = MaterialTheme.typography.titleMedium)
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SyncToggle(label: String, detail: String, checked: Boolean, disabled: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 14.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, enabled = !disabled, onCheckedChange = onChange)
    }
}

@Composable
private fun DividerLine() {
    val line = MaterialTheme.colorScheme.outline
    androidx.compose.foundation.Canvas(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp)) {
        drawLine(line, androidx.compose.ui.geometry.Offset(0f, 0f), androidx.compose.ui.geometry.Offset(size.width, 0f), 1.dp.toPx())
    }
}

@Composable
private fun PrivacyNote(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
