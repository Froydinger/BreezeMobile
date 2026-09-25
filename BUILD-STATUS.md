# Android development status

## September 24 direct-download release 0.1.9

- Published signed release [`Breeze-Android-0.1.9-beta.1-arm64.apk`](https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.9-beta.1/Breeze-Android-0.1.9-beta.1-arm64.apk) at [GitHub release android-v0.1.9-beta.1](https://github.com/Froydinger/BreezeMobile/releases/tag/android-v0.1.9-beta.1), versionCode 10. SHA-256: `16f99a7c6c1f1a7504e869ea5b772597c16ca92ec192c1c8d11e68a68db2c73a`. Signature fingerprint matches the existing production signer: `8448bcee588513337474ea4ae5f098fdbf35db3644ad6abd01d93be28561c628`.
- Changes: the active-page URL bar is flatter with teal URL text; theme-specific launcher icons hand off through a stable entry activity so theme changes keep the browser open.
- Build checks passed: `:app:testDebugUnitTest`, `:app:assembleDebug`, `:app:assembleRelease`, APK alignment, and signature verification. This is not exhaustive feature testing.
- Breeze Dev was updated in place on Pixel 11 to versionCode 10 / versionName 0.1.9-dev. Switching Light → Dark kept process PID 23283 and `MainActivity` resumed. The production `com.froydinger.breeze` app remains at 0.1.8; it was not installed or modified.
- The public update manifest now points to 0.1.9 at [`update/latest.json`](https://raw.githubusercontent.com/Froydinger/BreezeMobile/main/update/latest.json). Its release asset returned HTTP 200. The existing app checks the manifest at cold launch and caches successful checks for up to 12 hours.
- The Android lander links now point to 0.1.9. Website commit: `Froydinger/breezebrowser` `1dca07c`; Netlify production deploy: `6ab5ca6927a4291215df9d44` on `breezebrowser`. Both [breeze.froydinger.design/mobile](https://breeze.froydinger.design/mobile/) and [breeze.froydingermedia.online/mobile](https://breeze.froydingermedia.online/mobile/) returned HTTP 200 with the 0.1.9 APK link.
- The PWA install flow is still shortcut-based and is deliberately deferred to the next app update. No Breeze Cloud Worker files changed.

## September 24 direct-download release 0.1.8

- Published signed release [`Breeze-Android-0.1.8-beta.1-arm64.apk`](https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.8-beta.1/Breeze-Android-0.1.8-beta.1-arm64.apk) at [GitHub release android-v0.1.8-beta.1](https://github.com/Froydinger/BreezeMobile/releases/tag/android-v0.1.8-beta.1). SHA-256: `d24175196c8de7cdb81341955945d94b32329ba20ac1be185c39f5d75eb072fe`. APK signature fingerprint matches the existing production signer: `8448bcee588513337474ea4ae5f098fdbf35db3644ad6abd01d93be28561c628`.
- The production app is on Pixel 11 at versionCode 9 / versionName 0.1.8. Breeze Dev is installed at versionCode 9 / versionName 0.1.8-dev. Both were updated in place. Future Pixel installs should target Breeze Dev; the user will update production Breeze through its in-app update flow.
- Added a Git-tracked release manifest at [`update/latest.json`](https://raw.githubusercontent.com/Froydinger/BreezeMobile/main/update/latest.json). A cold launch fetches this small public manifest, compares versionCode, and caches successful checks for 12 hours (failed checks retry hourly). The app only offers a download when a higher version is published. The current 0.1.8 build correctly sees itself as current, so the automatic newer-version prompt has not yet been exercised; the user will test it with a later release.
- The version prompt downloads the signed APK through Android Download Manager. The user confirmed the tested prompt/download flow works. Android's download notification then opens the system installer for the user's install confirmation; installation is not silent.
- Build checks passed: `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleRelease`. `git diff --check` passed. This is not a claim of exhaustive feature or device testing.
- The Android lander now points to 0.1.8 and explains the update check. Site commit: `Froydinger/breezebrowser` `f5ac12a`; production deploy: `6ab5b031aaaf047b071dda70` on the verified `breezebrowser` Netlify site. Both [breeze.froydinger.design/mobile](https://breeze.froydinger.design/mobile/) and [breeze.froydingermedia.online/mobile](https://breeze.froydingermedia.online/mobile/) returned HTTP 200 with the 0.1.8 listing.
- No Cloudflare Worker files changed.

## September 24 direct-download release 0.1.7

- Production app `com.froydinger.breeze` is installed on Pixel 11 at versionCode 8 / versionName 0.1.7, updated in place with `adb install -r`.
- Signed APK: [`Breeze-Android-0.1.7-beta.1-arm64.apk`](https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.7-beta.1/Breeze-Android-0.1.7-beta.1-arm64.apk). SHA-256: `c74805207a0542459a5a0de78198748d1a6ba1ceb47d2589c4aa127b42638756`. Signature identity matches the existing 0.1.6 release.
- Fixes: system theme changes are handled without recreating the browser activity; Nav's Reminder tool submits reminder requests for local parsing/scheduling rather than always opening the manual form; parser accepts “set a reminder …” wording and removes the leading “to” from the saved title.
- Pixel theme check: switched in-app System → Light → Dark → System and changed system night mode Light → Dark → Auto. The production process stayed at PID 25539 throughout; no Android runtime exception appeared in the sampled log.
- Build checks: `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleRelease` passed. This is not a claim of exhaustive device/site testing.
- At 0.1.7, Breeze Dev had a static “Time to update” prompt to validate the download UX. Release 0.1.8 replaced it with the Git-tracked version check documented above.
- Breeze website lander links were updated to this asset and deployed to the verified `breezebrowser` Netlify site. No Breeze Cloud Worker files changed.

## September 24 direct-download release 0.1.6

- Production app `com.froydinger.breeze` is installed on Pixel 11 at versionCode 7 / versionName 0.1.6. Updated in place with `adb install -r`; no uninstall was used.
- Signed APK: [`Breeze-Android-0.1.6-beta.1-arm64.apk`](https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.6-beta.1/Breeze-Android-0.1.6-beta.1-arm64.apk). SHA-256: `a517e2f3b876d44c1c31b5e0b2910d63ef43a4446328afd10574aed0b62ed878`. APK signature and alignment passed; signing certificate matches the existing release identity.
- Android source is pushed at `Froydinger/BreezeMobile` commit `1e4a2e1`. `:app:testDebugUnitTest` and `:app:assembleRelease` succeeded. Unit tests cover bookmark/password export parsing, credential origin checks, and reminder scheduling/delivery policy.
- Pixel smoke check: Breeze launched at version 0.1.6, stayed foregrounded, and the active system PiP video changed frames in successive screenshots. No fatal exception or signal appeared in the sampled app log.
- The Android download buttons and version label are live at [breeze.froydinger.design/mobile](https://breeze.froydinger.design/mobile/). Site commit: `Froydinger/breezebrowser` `03796f1`; Netlify production deploy succeeded and both the page and APK link returned successfully.
- Still not verified end to end: importing real export files, biometric save/fill on a live website, reminder delivery at scheduled time, microphone/location permission flows, external-app handoff across many apps, and broad device/site coverage. These tests are not claimed as passed.
- No Worker files changed in this release. Cloud sync remains marked coming soon.

## September 23 verification

- The existing Breeze Cloud chat Worker serves the mobile response route. Deployed version: `6ec70490-4311-404f-ac9a-ab90f2cef16c`; configured model: `gpt-6-luna` with web search.
- Five live Cloud checks passed for chat, research, fact-check, summarize, and YouTube. The completed responses and event summaries are in [cloud-results.json](qa/cloud-results.json).
- Emulator checks covered a conversational `hey`, research with clickable citations and source links, and `/summarize` on example.com using actual rendered page text. The chat composer remained visible with the keyboard open.
- Dark/light theme and screen captures are available under [qa/screenshots](qa/screenshots), including `final-keyboard-light.png`, `final-research-result-dark.png`, and `final-summary-light.png`. Older unprefixed captures include intermediate failures and are not final acceptance evidence.
- Independent Luna-agent visual review passed the revised reference screens; see [VISUAL-REVIEW.md](qa/VISUAL-REVIEW.md). The current build was installed on Pixel 11 over wireless ADB at `192.168.12.230:37113`; the app process was present and no current-PID fatal exception was found in the sampled log. The phone was at its lock screen during this pass, so current-build visual behavior on Pixel is not verified. Emulator screenshots confirm the page preview morph in both directions. These checks do not claim exhaustive feature verification.
- Emulator camera check passed Android permission, origin-specific site consent, and live video. Local vault authentication and saving a synthetic login passed. Microphone/location and website autofill are implemented but have not yet received equivalent end-to-end checks.
- The APK is a private development build containing a private debug credential. Do not distribute it publicly or through Play.
- The local password vault is encrypted with Android Keystore and requires device authentication. Website passkey provider authorization remains a release requirement; no incomplete provider toggle is exposed.

No broader release readiness is implied by the checks above.


## Reviewed APK

- SHA-256: `af35836dbd7bc7290f29f8e40e326991b6a698c47a174f7e412741be5673186a`
- Build: `:app:testDebugUnitTest` and `:app:assembleDebug` succeeded; a full Android end-to-end suite was not run.
- Install preserved the existing app package and used `adb install -r`.
