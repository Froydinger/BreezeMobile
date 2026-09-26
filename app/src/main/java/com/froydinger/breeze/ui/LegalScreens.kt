package com.froydinger.breeze.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private data class LegalSection(val title: String, val text: String)

@Composable
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = "Privacy policy",
        intro = "Effective September 25, 2026. Breeze Mobile keeps browser data on your device by default. This policy explains what the app stores, what it sends for features you use, and how to manage your account data.",
        sections = listOf(
            LegalSection("Data stored on your device", "Breeze stores browser history, bookmarks, open tabs, settings, reminders, and Aero chat history locally by default. App state and the account session are protected with Android Keystore-backed encryption. Cookies and other website storage are kept in the browser profile. The password vault uses a separate encrypted store that requires device authentication; saved website passwords are never included in cloud sync."),
            LegalSection("Websites and permissions", "Websites you visit receive ordinary connection data and anything you choose to enter or share with them. They may store cookies and other site data in the browser. A site may request access to features such as your camera, microphone, or location; Android and Breeze let you decide whether to allow it. Websites may offer passkeys through Android and your selected credential provider. Availability for websites inside Breeze depends on Android, the credential provider, and Google's browser-app approval. Breeze does not receive or sync those private keys."),
            LegalSection("Breeze account", "You can sign in with email and password or Google. Supabase Auth receives the sign-in details needed to operate your account; Google shares basic account identity such as your email address when you choose Google sign-in. Breeze does not receive your Google password. Your Breeze session is stored encrypted on this device. Breeze account passkeys are optional; their private keys remain with Android and your selected credential provider."),
            LegalSection("Optional cloud sync", "Bookmarks, open tab URLs and titles, browsing history, Aero text and source links, and reminders each have a separate sync switch and start off. When you turn on a category, its data is sent over HTTPS to your account in Supabase. Synced data is not end-to-end encrypted by Breeze. Turning a category off stops future syncing but does not delete the existing cloud copy. Signing out removes synced data from this device; it does not delete the cloud copy. Deleting your account removes its synced data."),
            LegalSection("Aero, images, and voice", "When you send Aero a request, Breeze sends your prompt and relevant chat context to the Breeze Cloud service on Cloudflare, which may send it to OpenAI to generate a response. When page context is attached, the page title, URL, and readable text may be included. An image is sent only when you submit it with a request. If you use voice input, the recording is sent to Breeze Cloud for transcription and the resulting text is used as your prompt. These providers process data under their own terms and privacy policies."),
            LegalSection("Reminders and notifications", "Reminders are stored and scheduled on your device. If you sign in, enable Reminders sync, and allow notifications, Breeze can register this app installation with Firebase Cloud Messaging for reminder alerts. For a cloud alert, Firebase receives the app installation identifier and an opaque reminder identifier and due time; the reminder text is fetched from your Breeze account after delivery and is not included in the push payload. Reminder delivery depends on Android notification settings, Google Play services, and network access. A device may also receive a local reminder notification from Android without cloud push."),
            LegalSection("Export, deletion, and retention", "Use Export my data to save a copy of browser data. Signing out clears synced copies from this device but leaves account data in the cloud and keeps the local password vault. Turning off a sync category does not remove its cloud copy. Delete account permanently deletes the account and its synced data; it cannot be undone. You can clear local browsing data from Breeze settings. Data sent to third-party providers is handled under their retention rules."),
            LegalSection("Use and sharing", "Breeze does not sell personal data or use it for advertising. Firebase Analytics is disabled. Breeze shares information with Google, Supabase, Cloudflare, Firebase, OpenAI, or the website you choose to visit only as needed for the feature or connection you use. Breeze does not sync cookies, site storage, or the password vault."),
            LegalSection("Updates and contact", "Breeze may check GitHub for app updates; this check does not include your browsing history or password vault. Questions or privacy requests: jake@winthenight.info."),
        ),
        onBack = onBack,
    )
}

@Composable
fun TermsOfServiceScreen(onBack: () -> Unit) {
    LegalDocumentScreen(
        title = "Terms of service",
        intro = "Effective September 25, 2026. These terms apply when you use Breeze Mobile. Creating an account requires your agreement to these terms and the Privacy Policy.",
        sections = listOf(
            LegalSection("Using Breeze", "You may use Breeze to browse the web, manage browser data, use Aero, and set reminders. Use the app lawfully, protect access to your device and account, and do not interfere with the app or its services."),
            LegalSection("Accounts and cloud sync", "An account is optional. Sync categories start off and are sent to your Breeze account only when you enable them. You can export data, sign out, change sync choices, or delete your account. Turning off a category does not delete its existing cloud copy. The password vault remains on your device and is not synced."),
            LegalSection("Websites, passkeys, and permissions", "Websites and credential providers are third-party services with their own terms and privacy practices. You choose whether to grant site permissions. Passkey availability depends on the website, Android, your selected credential provider, and any required Google approval for browser use. Breeze does not operate those sites or providers, and passkey private keys stay with your credential provider."),
            LegalSection("Aero and AI responses", "Aero uses AI services and may return inaccurate, incomplete, or outdated information. Sources may be shown when available; check important claims yourself. Do not rely on Aero for emergencies or as a substitute for professional medical, legal, or financial advice."),
            LegalSection("Reminders", "Breeze may schedule reminders locally and, when enabled and available, use Firebase Cloud Messaging for synced reminder alerts. Push messages contain an opaque reminder identifier and due time; the app fetches reminder text from your Breeze account after delivery. Delivery can be affected by Android permissions, battery settings, connectivity, or service availability. Do not rely on Breeze reminders for urgent or safety-critical events."),
            LegalSection("Availability and changes", "Breeze and connected services may change or be unavailable. We may update these terms as features change. We will show an updated date when this in-app document changes. Continued use after an updated version is shown means you accept the revised terms."),
            LegalSection("Contact", "Questions about these terms: jake@winthenight.info."),
        ),
        onBack = onBack,
    )
}

@Composable
private fun LegalDocumentScreen(
    title: String,
    intro: String,
    sections: List<LegalSection>,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize().then(screenEntrance())) {
        Row(Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            IconButton(onClick = onBack) { Icon(BreezeIcons.ArrowBack, "Back") }
            Text(title, Modifier.padding(start = 8.dp), style = MaterialTheme.typography.headlineSmall)
        }
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item { Text(intro, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface) }
            items(sections) { section ->
                Column {
                    Text(section.title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(5.dp))
                    Text(section.text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
