# Android development status

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
