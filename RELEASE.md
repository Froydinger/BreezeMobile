# Release and service handoff

## Who owns what

- **Android app:** this `BreezeMobile` repository.
- **Breeze Cloud mobile API:** `Froydinger/breezebrowser-live`, in `cloudflare/breeze-chat-worker`; the app calls `/v1/mobile/responses`. Its current protocol notes are copied to [`cloud/MOBILE-RESPONSES.md`](cloud/MOBILE-RESPONSES.md) for app-side reference. Keep the copy and Worker documentation aligned when the protocol changes.
- **Website and Android lander:** the separate `Froydinger/breezebrowser` site repository, at `/mobile/`. The lander links to a versioned APK release; it does not build or sign the app.

The existing live lander still points to the 0.1.5 beta asset in `breezebrowser-live`. Leave that working link in place until a new signed APK is published from this repository; then move the lander to the new versioned asset as part of that release.

## Publishing a new Android beta

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`. Keep the Android signing identity and production credentials outside Git.
2. Build and sign the arm64 APK using the configured release signing setup. Verify the APK signature and SHA-256 before publishing.
3. Create a versioned GitHub release in this repository and attach the signed APK. Keep the asset name and version label consistent.
4. Install that exact APK over the existing Pixel app and confirm the installed version and data preservation. Do not uninstall for routine updates.
5. In the website repository, update the APK URL and version label in `mobile/index.html` and every matching Android download/structured-data URL in `index.html`. Deploy using its established website workflow.
6. Open the live `/mobile/` page and download the linked APK once to verify the public path.

Worker changes are separate. Test and deploy the Worker from `breezebrowser-live` before relying on new API behavior; do not bundle a Worker deploy into an Android release by assumption.

## Local build credential

For local builds, put the authorized Breeze client credential in an ignored root `.breeze-client-token` file or provide it through `BREEZE_CLIENT_TOKEN`. Debug builds may omit it for UI/build checks; cloud requests will not authenticate without it. Release builds require it. Never put the value in command output, source control, screenshots, or public release notes.
