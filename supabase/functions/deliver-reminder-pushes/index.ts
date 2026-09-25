import { createClient } from "npm:@supabase/supabase-js@2";

type ServiceAccount = {
  project_id: string;
  client_email: string;
  private_key: string;
};

type PushTarget = {
  user_id: string;
  reminder_id: string;
  due_at: string;
  installation_id: string;
};

const encoder = new TextEncoder();
let cachedAccessToken = "";
let cachedAccessTokenExpiresAt = 0;

Deno.serve(async (request) => {
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const serviceRole = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
  const bearer = request.headers.get("Authorization")?.match(/^Bearer\s+(.+)$/i)?.[1] ?? "";
  if (!bearer || bearer.length > 256 || !serviceRole) {
    return json({ error: "Unauthorized" }, 401);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
  const firebaseServiceAccountJson = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON") ?? "";
  if (!supabaseUrl) {
    return json({ error: "Reminder push delivery is not configured." }, 503);
  }

  const admin = createAdminClient(supabaseUrl, serviceRole);
  const { data: authorized, error: authorizationError } = await admin.rpc(
    "is_breeze_reminder_scheduler_token_valid",
    { p_token: bearer },
  );
  if (authorizationError || authorized !== true) {
    return json({ error: "Unauthorized" }, 401);
  }
  if (!firebaseServiceAccountJson) {
    return json({ error: "Reminder push delivery is not configured." }, 503);
  }

  let serviceAccount: ServiceAccount;
  try {
    serviceAccount = JSON.parse(firebaseServiceAccountJson) as ServiceAccount;
    if (!serviceAccount.project_id || !serviceAccount.client_email || !serviceAccount.private_key) {
      return json({ error: "Firebase sender credentials are incomplete." }, 503);
    }
    if (serviceAccount.project_id !== "breeze-mobile-d26fe") {
      return json({ error: "Firebase sender project does not match Breeze Mobile." }, 503);
    }
  } catch {
    return json({ error: "Firebase sender credentials are invalid." }, 503);
  }

  try {
    const { data: targets, error } = await admin.rpc("get_breeze_due_reminder_push_targets");
    if (error) return json({ error: "Due reminders could not be checked." }, 500);
    const candidates = (targets ?? []) as PushTarget[];
    if (candidates.length === 0) return json({ sent: 0, failed: 0 }, 200);

    const accessToken = await googleAccessToken(serviceAccount);
    let sent = 0;
    let failed = 0;

    for (const target of candidates) {
      if (!validTarget(target)) continue;
      const dueAt = new Date(target.due_at);
      if (!Number.isFinite(dueAt.getTime())) continue;

      const key = {
        p_user_id: target.user_id,
        p_reminder_id: target.reminder_id,
        p_due_at: dueAt.toISOString(),
        p_installation_id: target.installation_id,
      };
      const claim = await admin.rpc("claim_breeze_reminder_push_delivery", key);
      if (claim.error) {
        failed += 1;
        continue;
      }
      if (claim.data !== true) continue;

      const response = await fetch(
        `https://fcm.googleapis.com/v1/projects/${serviceAccount.project_id}/messages:send`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            message: {
              fid: target.installation_id,
              data: {
                type: "breeze_reminder",
                reminder_id: target.reminder_id,
                due_at: String(dueAt.getTime()),
              },
              android: { priority: "HIGH" },
            },
          }),
        },
      );

      if (response.ok) {
        const completed = await admin.rpc("complete_breeze_reminder_push_delivery", key);
        if (completed.error) failed += 1;
        else sent += 1;
      } else {
        const body = await response.json().catch(() => ({}));
        if (isUnregisteredFid(body)) {
          await admin.from("breeze_push_devices")
            .delete()
            .eq("user_id", target.user_id)
            .eq("installation_id", target.installation_id);
          await admin.rpc("complete_breeze_reminder_push_delivery", key);
        } else {
          await admin.rpc("release_breeze_reminder_push_delivery", key);
        }
        failed += 1;
      }
    }

    return json({ sent, failed }, 200);
  } catch {
    return json({ error: "Reminder push delivery could not complete." }, 500);
  }
});

function createAdminClient(url: string, serviceRole: string) {
  return createClient(url, serviceRole, {
    auth: { persistSession: false, autoRefreshToken: false },
  });
}

async function googleAccessToken(account: ServiceAccount): Promise<string> {
  if (cachedAccessToken && cachedAccessTokenExpiresAt > Date.now() + 60_000) {
    return cachedAccessToken;
  }

  const issuedAt = Math.floor(Date.now() / 1000);
  const header = base64UrlJson({ alg: "RS256", typ: "JWT" });
  const claims = base64UrlJson({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: issuedAt,
    exp: issuedAt + 3600,
  });
  const unsignedJwt = `${header}.${claims}`;
  const privateKey = await importPrivateKey(account.private_key);
  const signature = new Uint8Array(await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    privateKey,
    encoder.encode(unsignedJwt),
  ));
  const assertion = `${unsignedJwt}.${base64UrlBytes(signature)}`;

  const response = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const body = await response.json().catch(() => ({}));
  if (!response.ok || typeof body.access_token !== "string") {
    throw new Error("Firebase authorization failed.");
  }

  cachedAccessToken = body.access_token;
  cachedAccessTokenExpiresAt = Date.now() + Math.min(Number(body.expires_in) || 3600, 3500) * 1000;
  return cachedAccessToken;
}

async function importPrivateKey(pemValue: string): Promise<CryptoKey> {
  const pem = pemValue.replace(/\\n/g, "\n");
  const base64 = pem.replace(/-----BEGIN PRIVATE KEY-----|-----END PRIVATE KEY-----|\s/g, "");
  const binary = atob(base64);
  const der = Uint8Array.from(binary, (character) => character.charCodeAt(0));
  return await crypto.subtle.importKey(
    "pkcs8",
    der,
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
}

function validTarget(target: PushTarget): boolean {
  return typeof target.user_id === "string" &&
    typeof target.reminder_id === "string" &&
    target.reminder_id.length > 0 && target.reminder_id.length <= 128 &&
    typeof target.installation_id === "string" &&
    /^[A-Za-z0-9_-]{20,64}$/.test(target.installation_id) &&
    typeof target.due_at === "string";
}

function isUnregisteredFid(value: unknown): boolean {
  if (!value || typeof value !== "object") return false;
  const error = (value as Record<string, unknown>).error;
  if (!error || typeof error !== "object") return false;
  const details = (error as Record<string, unknown>).details;
  if (!Array.isArray(details)) return false;
  return details.some((detail) => {
    if (!detail || typeof detail !== "object") return false;
    return (detail as Record<string, unknown>).errorCode === "UNREGISTERED";
  });
}

function base64UrlJson(value: unknown): string {
  return base64UrlBytes(encoder.encode(JSON.stringify(value)));
}

function base64UrlBytes(value: Uint8Array): string {
  let binary = "";
  for (const byte of value) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_");
}

function json(body: Record<string, unknown>, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store" },
  });
}
