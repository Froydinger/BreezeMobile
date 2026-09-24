# Android client contract: mobile Responses

This file summarizes what the Android client expects. The Worker implementation and deployment live in [`Froydinger/breezebrowser-live`](https://github.com/Froydinger/breezebrowser-live/tree/native-swift-browser/cloudflare/breeze-chat-worker). Check that repository and the live deployment before changing or relying on server behavior; this note is not deployment evidence.

## Request

The app posts JSON to `https://breeze-chat.jakefroydinger.workers.dev/v1/mobile/responses` with bearer authorization and `Accept: text/event-stream`:

```json
{
  "chatId": "chat-id",
  "turnId": "turn-id",
  "runId": "run-id",
  "idempotencyKey": "retry-key",
  "task": "chat",
  "input": "Question text",
  "context": "Optional selected page text",
  "image": "Optional image data URL"
}
```

The task is one of `chat`, `research`, `summarize`, `factcheck`, or `youtube`. `image` is optional. The Worker validates and bounds inputs; server-side limits and task behavior must be read from its current implementation.

## Server-sent events

Each `data:` event is a JSON envelope with `v: 1`, the matching `runId`, a strictly increasing request-local `eventId`, and `type`. The Android parser recognizes `accepted`, `status`, `tool_started`, `source`, `text_delta`, `citation`, `completed`, and `failed`; it also safely represents `cancelled` and unknown event names. Events must be separated using standard blank-line SSE framing. Completion is terminal; malformed envelopes, wrong run IDs, non-increasing IDs, non-SSE responses, or premature transport failure are reported as errors by the app.

## Change rule

If request fields, size limits, task names, authentication, or event envelopes change, update both this client and the Worker repository together. Verify with a Worker-level contract check and at least one installed-app request for each affected task before shipping. A successful Android build alone does not verify the service integration.
