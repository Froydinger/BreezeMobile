# Android development status

## September 24 installed beta and remaining checks

- Pixel regular app is `com.froydinger.breeze`, versionCode 6 / versionName 0.1.5. Its installed `base.apk` SHA-256 is `a33951967c8300013e0e3adb8b9c357c9f391c594037fc0ef502ba3ba261f9c0`, exactly matching the signed GitHub release asset `Breeze-Android-0.1.5-beta.1-arm64.apk`. The installed binary is current for the Android source at commit `8127b5d`.
- The Pixel also has a separate older development install, `com.froydinger.breeze.dev`, versionCode 5 / versionName 0.1.4-dev.
- The only GitHub commit ahead of the old monorepo checkout changes desktop files; it does not change Android. Android source is now maintained in the separate `BreezeMobile` repository.
- Checked-in automated tests currently cover the reminder request parser only. Existing QA records cover specific emulator flows, not full feature coverage.
- Still lacking equivalent end-to-end checks: importing real bookmark/password exports; microphone and location permission flows; website password autofill; reminder notification timing/recurrence; picture-in-picture and external app handoff; and broad physical-device/site coverage.
- Current local-only Worker edits in the old monorepo have not been tested or deployed. The installed APK continues using the live Worker and has not been changed by those edits.

No broader release readiness is implied by the checks above.

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
