# BreezeMobile working rules

- This repository is the canonical source for the Android app. Do not make Android changes in the `android/` snapshot inside `breezebrowser-live`.
- Check the current branch and dirty files before changing or releasing anything.
- Treat `.breeze-client-token`, signing keys, and signing credentials as secrets. Keep the token file ignored, never print its value, and never commit it.
- The mobile Breeze Cloud endpoint lives in `Froydinger/breezebrowser-live`, under `cloudflare/breeze-chat-worker`. If changing request fields or SSE events, update and verify both repositories together. A mobile build does not deploy the Worker.
- The Android download lander lives in the separate `Froydinger/breezebrowser` website repository, at `/mobile/`. For each public APK release, update every versioned APK URL and version label on the lander, deploy it through that repo's existing workflow, and verify the live page and download.
- Keep Android app source, Worker deployment, APK release, and website deployment as separate steps. A successful build or GitHub release does not prove the live Worker or lander changed.
- Do not start an Android emulator unless the task needs one; use the connected Pixel for device checks when requested.
