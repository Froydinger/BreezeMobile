# Release and service handoff

## Who owns what

- **Android app:** this `BreezeMobile` repository.
- **Breeze Cloud mobile API:** `Froydinger/breezebrowser-live`, in `cloudflare/breeze-chat-worker`; the app calls `/v1/mobile/responses`. Its current protocol notes are copied to [`cloud/MOBILE-RESPONSES.md`](cloud/MOBILE-RESPONSES.md) for app-side reference. Keep the copy and Worker documentation aligned when the protocol changes.
- **Website and Android lander:** the separate `Froydinger/breezebrowser` site repository, at `/mobile/`. The lander links to a versioned APK release; it does not build or sign the app.

Keep the live lander pointed at the latest published signed APK until the next signed APK is attached to a versioned GitHub release. Then update the Android URLs and version label in the separate website repository as part of the same release.

## Publishing a new Android beta

1. Update `versionCode` and `versionName` in `app/build.gradle.kts`. Keep the Android signing identity and production credentials outside Git.
2. Build and sign the arm64 APK using the configured release signing setup. Verify the APK signature and SHA-256 before publishing.
3. Create a versioned GitHub release in this repository and attach the signed APK. Keep the asset name and version label consistent; put `Android-Version-Code: <number>` in the release notes for traceability.
4. After the release asset is live, update `update/latest.json` on `main` with that release's version code, version name, tag, APK filename, and exact GitHub download URL. Current Android builds check this public manifest on each cold app launch, use the cached result if offline, and offer the APK only when its version code is higher.
5. Test device changes in Breeze Dev. Do not install a release over production Breeze on the Pixel; the user updates production through the in-app update flow.
6. In the website repository, update the APK URL and version label in `mobile/index.html` and every matching Android download/structured-data URL in `index.html`. Deploy to the verified Netlify project `breezebrowser` (site ID `bb1061f6-da1e-47b4-b274-039cab2281e8`) explicitly; verify the project and domain before deploying.
7. Open the live `/mobile/` page and verify the current version and linked APK URL.

Worker changes are separate. Test and deploy the Worker from `breezebrowser-live` before relying on new API behavior; do not bundle a Worker deploy into an Android release by assumption.

## Local build credential

For local builds, put the authorized Breeze client credential in an ignored root `.breeze-client-token` file or provide it through `BREEZE_CLIENT_TOKEN`. Account builds also need the Supabase public anon key through `SUPABASE_ANON_KEY` or an ignored root `.supabase-anon-key` file. The anon key is designed to ship in the app; keep the service-role key and Firebase service-account JSON only in Supabase Secrets. Debug builds may omit the Breeze client credential for UI/build checks; cloud requests will not authenticate without it. Release builds require the Breeze client credential. Never put private credentials in command output, source control, screenshots, or public release notes.
