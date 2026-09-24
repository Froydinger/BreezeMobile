# Breeze Android

Native Kotlin/Compose browser using Android System WebView (Chromium) for web pages. The current signed download is the direct-release beta, not a Google Play release.

## Current scope

- Ask-first routing, browser navigation, tabs and private tabs, local history, bookmarks, and saved chats.
- Dark and light themes, configurable glass surfaces, wallpapers, tab wall, and local settings.
- Android Keystore encrypted app records and a separate device-authenticated local password vault.
- Chromium page protection, site dialogs and file inputs, find and desktop-site controls, and SAF downloads.
- Breeze Cloud chat, research, fact-check, summarize, and YouTube tasks use the mobile endpoint maintained in [`Froydinger/breezebrowser-live`](https://github.com/Froydinger/breezebrowser-live/tree/native-swift-browser/cloudflare/breeze-chat-worker). Keep its request/SSE contract in sync with `NavSseClient.kt` and `cloud/MOBILE-RESPONSES.md`.
- **Cloud sync — Coming soon.** There is no account creation or cloud-sync connection in this build. Local password storage does not upload credentials.

## Build

Requires JDK 17, SDK platform 37.1, build tools 36, and the checked-in Gradle wrapper. Target SDK is 36. Packaging currently targets arm64 devices.

```sh
JAVA_HOME=/opt/homebrew/opt/openjdk@17 \
ANDROID_HOME=/opt/homebrew/share/android-sdk \
./gradlew :app:assembleDebug
```

Debug and release builds read an ignored root `.breeze-client-token` or `BREEZE_CLIENT_TOKEN`. Never commit or print this credential. The current beta embeds a shared Worker client token in the APK, so it is extractable and is only a transitional beta authorization scheme; it is not account authentication. Replace that scheme before broader production distribution. No provider API key is stored in the app. Use [RELEASE.md](RELEASE.md) for the signed APK and website handoff.

Install with `adb -s DEVICE install -r app/build/outputs/apk/debug/app-debug.apk`. The development package is `com.froydinger.breeze.dev`, separate from a future production package.

## Evidence and limits

Five live Worker checks for chat, research, fact-check, summarize, and YouTube completed successfully; results are recorded in [cloud-results.json](qa/cloud-results.json). Emulator checks also covered conversational chat, clickable research citations and source links, page summarization using rendered text from example.com, and the keyboard-visible chat composer. Theme and screen captures are in [qa/screenshots](qa/screenshots).

Independent visual review passed the revised reference screens (see `qa/VISUAL-REVIEW.md`). Functional interaction evidence is from the emulator unless explicitly stated in [BUILD-STATUS.md](BUILD-STATUS.md). These checks cover the described flows, not every feature or device configuration.

Website passkey provider authorization remains a release requirement; there is no passkey toggle claiming that provider setup is complete. The local password vault uses encrypted device storage and system authentication. The temporary encrypted JSON app store must become transactional indexed storage before large-history scaling or sync.

## Files

- `app/src/main/java/com/froydinger/breeze/BrowserState.kt`: local browser/session/chat orchestration.
- `ui/`: shared optical surfaces, library/settings/chat, and password vault.
- `data/`: encrypted records and separate auth-bound vault.
- `cloud/`: bounded HTTPS SSE protocol client.
- `deferred/desktop/`: future Swift sync wire format; excluded from desktop build.
- [`RELEASE.md`](RELEASE.md): release steps and the website/Worker handoff.
- [`cloud/MOBILE-RESPONSES.md`](cloud/MOBILE-RESPONSES.md): the mobile route and protocol contract copied into this repo for app-side context; the Worker implementation remains in `breezebrowser-live`.
- `qa/`: emulator captures, test records, and bounded visual/performance reviews.
- `design-assets/`: app iconography and visual resources.
