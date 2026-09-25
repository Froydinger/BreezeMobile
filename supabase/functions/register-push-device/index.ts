import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

Deno.serve(async (request) => {
  if (request.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (request.method !== "POST") return json({ error: "Method not allowed" }, 405);

  const authorization = request.headers.get("Authorization") ?? "";
  const token = authorization.match(/^Bearer\s+(.+)$/i)?.[1];
  if (!token) return json({ error: "Sign in to manage reminder alerts." }, 401);

  let installationId: string;
  let enabled: boolean;
  try {
    const body = await request.json();
    installationId = typeof body.installationId === "string" ? body.installationId : "";
    enabled = body.enabled === true;
  } catch {
    return json({ error: "Invalid request." }, 400);
  }
  if (!/^[A-Za-z0-9_-]{20,64}$/.test(installationId)) {
    return json({ error: "Invalid Firebase installation ID." }, 400);
  }

  const url = Deno.env.get("SUPABASE_URL");
  const anonKey = Deno.env.get("SUPABASE_ANON_KEY");
  if (!url || !anonKey) return json({ error: "Reminder alerts are temporarily unavailable." }, 503);

  const client = createClient(url, anonKey, {
    auth: { persistSession: false, autoRefreshToken: false },
    global: { headers: { Authorization: authorization } },
  });
  const { data: authData, error: authError } = await client.auth.getUser(token);
  if (authError || !authData.user) return json({ error: "Your sign-in expired. Sign in again." }, 401);

  if (enabled) {
    const { data: preferences, error: preferenceError } = await client
      .from("breeze_sync_preferences")
      .select("reminders")
      .eq("user_id", authData.user.id)
      .maybeSingle();
    if (preferenceError) return json({ error: "Reminder sync could not be checked." }, 500);
    if (!preferences?.reminders) return json({ error: "Enable reminder sync before push alerts." }, 403);

    const { error } = await client.from("breeze_push_devices").upsert(
      { user_id: authData.user.id, installation_id: installationId, enabled: true },
      { onConflict: "user_id,installation_id" },
    );
    if (error) return json({ error: "This device could not be registered for reminder alerts." }, 500);
    return json({ registered: true }, 200);
  }

  const { error } = await client.from("breeze_push_devices")
    .delete()
    .eq("user_id", authData.user.id)
    .eq("installation_id", installationId);
  if (error) return json({ error: "This device could not be removed from reminder alerts." }, 500);
  return json({ registered: false }, 200);
});

function json(body: Record<string, unknown>, status: number) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}
