# Android development status

## September 25 direct-download release 0.1.15

- Published signed APK [Breeze-Android-0.1.15-beta.1-arm64.apk](https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.15-beta.1/Breeze-Android-0.1.15-beta.1-arm64.apk) at [GitHub release android-v0.1.15-beta.1](https://github.com/Froydinger/BreezeMobile/releases/tag/android-v0.1.15-beta.1), versionCode 16. SHA-256: 04c3e448503443e7a275bc34de9c68ada9fca1d91beb5ad48f77a528baf63dc7. APK v3 signing was verified; its certificate matches the production signer used by the existing app.
- Adds optional Breeze accounts with Google/email sign-in, separate opt-in sync for bookmarks, tabs, history, chats, and reminders, account passkeys, account export/deletion, and in-app Privacy Policy and Terms. Website passwords stay in the local encrypted vault.
- Nav now completes reminder setup in chat and asks follow-up questions only when date, time, or task details are missing. Local alarms remain available; when reminder sync and Android notifications are enabled, the Supabase minute scheduler can send an FCM alert containing only an opaque reminder ID and due time.
- Supabase auth, passkey RP/origins, reminder tables, scheduler, and sender secrets are configured. The cron endpoint has returned HTTP 200 with no due reminders. No real push was sent during this release pass.
- :app:testDebugUnitTest --rerun-tasks, :app:assembleDebug, and :app:assembleRelease passed. APK signature verification and git diff checks passed. Source commit: 292423c; updater manifest and lander publication are recorded below after their deployment checks.
- No emulator was started and the Pixel was not touched while it was offline. Actual passkey enrollment, notification delivery, and the in-app update flow on the phone remain for the user's later device check.

## September 25 direct-download release 0.1.14

- Published signed release [`Breeze-Android-0.1.14-beta.1-arm64.apk`](https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.14-beta.1/Breeze-Android-0.1.14-beta.1-arm64.apk) at [GitHub release android-v0.1.14-beta.1](https://github.com/Froydinger/BreezeMobile/releases/tag/android-v0.1.14-beta.1), versionCode 15. SHA-256: `85debbedb03dbb187dac64fe17e4b0bf64a6daacdef05bf1339ea84a10beaabe`. APK alignment and v3 signing passed; signer fingerprint matches the production signing identity.
- Adds a browser-side public-caption reader for YouTube Creator Breakdown. It reads YouTube's caption session in the active Chromium tab, sends up to 14,000 transcript characters as untrusted page context, and falls back honestly when captions cannot be read. No Cloudflare Worker files were changed or deployed.
- Pixel 11 Breeze Dev was updated to versionCode 15 / versionName 0.1.14-dev. A live TED video Creator Breakdown used transcript-specific details and clickable sources. The 0.1.13 device checks already verified page Summarize, Research, and Fact-check on Pixel 11. Main Breeze was not installed over; it remains at versionCode 13 / versionName 0.1.12.
- `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleRelease` passed. `git diff --check` passed. Full `:app:lintRelease` reports 13 existing errors in unchanged files (legacy API annotations, locale reads, activity casts, and camera feature declaration); this run did not modify those paths.
- Android source commit: `0c9f7c8`; update manifest commit: `c7f119a`. Breeze Dev checks the manifest on each cold launch; Main Breeze version 0.1.12 / versionCode 13 is eligible for this update.
- Android lander commit: `Froydinger/breezebrowser` `8a7bbcb`; production deploy `6ab62a624a87212c6fb14385` went to the verified `breezebrowser` Netlify site. Both [breeze.froydinger.design/mobile](https://breeze.froydinger.design/mobile/) and [breeze.froydingermedia.online/mobile](https://breeze.froydingermedia.online/mobile/) show 0.1.14 and link to the signed APK.

## September 25 direct-download release 0.1.13

- Published signed release [`Breeze-Android-0.1.13-beta.1-arm64.apk`](https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.13-beta.1/Breeze-Android-0.1.13-beta.1-arm64.apk) at [GitHub release android-v0.1.13-beta.1](https://github.com/Froydinger/BreezeMobile/releases/tag/android-v0.1.13-beta.1), versionCode 14. SHA-256: `0ad15a65a75987f03647a9d5fe0343497f0c69ebf2434ae0cdd77e8601310bf1`. Alignment and v3 signing passed; the signing fingerprint matches the existing production identity.
- Fixes Nav page actions so the selected page URL and rendered text are sent as attached context, while the visible chat bubble contains only the action. Adds distinct default requests and titles for Research, Summarize, Fact check, and YouTube.
- Verified on Pixel 11 in Breeze Dev against a Genuine Good article: Summarize returned article-specific content; Research and Fact check completed, with Fact check showing a clickable source. `:app:testDebugUnitTest`, debug/release assembly, release lint, and `git diff --check` passed.
- Breeze Dev is installed on Pixel 11 at versionCode 14 / versionName 0.1.13-dev. Main Breeze remains versionCode 13 / versionName 0.1.12 and was not installed over or modified.
- Android source commit: `7202951`; update manifest commit: `b56ed7e`. The public manifest returns versionCode 14 / version 0.1.13, and its APK URL returned HTTP 200 with the expected 238,713,738-byte asset.
- Android lander commit: `Froydinger/breezebrowser` `2084600`; production deploy `6ab6203a6b93b299f7da3b29` went to the verified `breezebrowser` Netlify site. Both [breeze.froydinger.design/mobile](https://breeze.froydinger.design/mobile/) and [breeze.froydingermedia.online/mobile](https://breeze.froydingermedia.online/mobile/) return the 0.1.13 version label and APK link.
- The mobile Cloudflare Worker was not changed.

## September 25 direct-download release 0.1.12

- Published signed release [`Breeze-Android-0.1.12-beta.1-arm64.apk`](https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.12-beta.1/Breeze-Android-0.1.12-beta.1-arm64.apk) at [GitHub release android-v0.1.12-beta.1](https://github.com/Froydinger/BreezeMobile/releases/tag/android-v0.1.12-beta.1), versionCode 13. SHA-256: `78a1fb6317a8f31c8b14a41f6d13cd7df96f1b573bbc729155e26b5533da96b5`. APK alignment and v3 signing passed; the signing fingerprint matches the existing production identity.
- Adds page-aware empty-chat quick actions in Nav, including Research, Summarize, Fact check, Reminder, and YouTube breakdown on YouTube videos. Selecting page text and opening Nav attaches that passage to the conversation.
- The PWA install request uses Android `WebAppManager` when available and surfaces install failures inside Breeze. A real PWA install on the Pixel has not been verified end to end.
- `:app:assembleRelease` and `:app:assembleDebug` passed. Breeze Dev was updated on Pixel 11 to versionCode 13 / versionName 0.1.12-dev; the Nav screen visibly showed Research, Summarize, Fact check, and Reminder for the attached ArcAI page. Production Breeze remains at versionCode 12 / versionName 0.1.11 and was not touched.
- Android source commit: `e22df40`; update manifest commit: `17e9e39`. The public manifest now points to 0.1.12 and returned HTTP 200; the signed APK asset returned HTTP 200.
- Android lander commit: `Froydinger/breezebrowser` `6b465a5`; production deploy: `6ab61125e02746065a1e7f51` on the verified `breezebrowser` Netlify site. Both [breeze.froydinger.design/mobile](https://breeze.froydinger.design/mobile/) and [breeze.froydingermedia.online/mobile](https://breeze.froydingermedia.online/mobile/) returned HTTP 200 with the 0.1.12 version label and APK link.
- No Cloudflare Worker files changed. No emulator was started.

## September 24 direct-download release 0.1.11

- Published signed release [`Breeze-Android-0.1.11-beta.1-arm64.apk`](https://github.com/Froydinger/BreezeMobile/releases/download/android-v0.1.11-beta.1/Breeze-Android-0.1.11-beta.1-arm64.apk) at [GitHub release android-v0.1.11-beta.1](https://github.com/Froydinger/BreezeMobile/releases/tag/android-v0.1.11-beta.1), versionCode 12. SHA-256: `dc6776246d8767cf96fe09228bfc95d77381ae9d2bdbeaacf098fdf375bc1251`. APK signature, alignment, and production signing fingerprint were checked.
- Fixes the theme-change/Recents lifecycle issue by resolving launcher-alias components from the manifest package and giving the launcher entry and browser activity distinct task affinities. Android Dev version 0.1.11-dev was installed on Pixel 11. Five launch → Home cycles left the browser task in the visible Recents list; no new post-fix fatal crash appeared in the sampled log. The old pre-fix crash remains in device history.
- Pixel production Breeze remains at versionCode 11 / versionName 0.1.10 and remains the default browser. It currently does not appear in the visible Recents list. The user will install 0.1.11 through the in-app update flow; the release APK was not installed over production.
- The PWA install flow now detects an HTTPS web app manifest and uses Android `WebAppManager` on API 37+, with Chrome as the fallback on older or unsupported versions. Pages without a manifest still use Add to Home screen. A real PWA install on the Pixel has not been tested end to end.
- Build and unit checks passed: `:app:testDebugUnitTest`, `:app:assembleDebug`, and `:app:assembleRelease`; APK alignment and signing verification passed. This is not exhaustive feature or website compatibility testing.
- Android source commit: `924c775`; update manifest commit: `7518741`. The public manifest points to version 0.1.11 and checks on each app start, using cached data offline.
- The Android lander links to 0.1.11. Website commit: `Froydinger/breezebrowser` `3045a56`; production deploy: `6ab5de04161104b59c109588` on the verified `breezebrowser` Netlify site. Both [breeze.froydinger.design/mobile](https://breeze.froydinger.design/mobile/) and [breeze.froydingermedia.online/mobile](https://breeze.froydingermedia.online/mobile/) returned the 0.1.11 version label and APK link.
- No Cloudflare Worker files changed. No emulator was started.

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
