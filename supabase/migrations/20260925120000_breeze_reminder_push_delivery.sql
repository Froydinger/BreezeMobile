create extension if not exists pg_cron;
create extension if not exists pg_net;

create table if not exists public.breeze_push_reminder_deliveries (
  user_id uuid not null references auth.users (id) on delete cascade,
  reminder_id text not null check (length(reminder_id) between 1 and 128),
  due_at timestamptz not null,
  installation_id text not null check (installation_id ~ '^[A-Za-z0-9_-]{20,64}$'),
  claim_until timestamptz not null default (now() + interval '5 minutes'),
  sent_at timestamptz,
  attempts integer not null default 1 check (attempts > 0),
  primary key (user_id, reminder_id, due_at, installation_id)
);

alter table public.breeze_push_reminder_deliveries enable row level security;
revoke all on public.breeze_push_reminder_deliveries from public, anon, authenticated;
grant select, insert, update, delete on public.breeze_push_reminder_deliveries to service_role;

create or replace function public.get_breeze_due_reminder_push_targets()
returns table (
  user_id uuid,
  reminder_id text,
  due_at timestamptz,
  installation_id text
)
language sql
security definer
set search_path = ''
stable
as $$
  with due_entries as (
    select
      preferences.user_id,
      entry.value ->> 'id' as reminder_id,
      case
        when (entry.value ->> 'dueAt') ~ '^[0-9]{12,13}$'
          then to_timestamp(((entry.value ->> 'dueAt')::numeric) / 1000.0)
        else null
      end as due_at
    from public.breeze_sync_preferences as preferences
    join public.breeze_sync_collections as collection
      on collection.user_id = preferences.user_id
     and collection.collection = 'reminders'
    cross join lateral jsonb_array_elements(
      case
        when jsonb_typeof(collection.payload -> 'entries') = 'array'
          then collection.payload -> 'entries'
        else '[]'::jsonb
      end
    ) as entry(value)
    where preferences.reminders is true
      and (entry.value ->> 'id') is not null
      and length(entry.value ->> 'id') between 1 and 128
      and (entry.value ->> 'dueAt') ~ '^[0-9]{12,13}$'
      and coalesce(entry.value ->> 'deliveredAt', 'null') = 'null'
      and not coalesce(collection.payload -> 'deletedIds' @> jsonb_build_array(entry.value ->> 'id'), false)
  )
  select
    due_entries.user_id,
    due_entries.reminder_id,
    due_entries.due_at,
    device.installation_id
  from due_entries
  join public.breeze_push_devices as device
    on device.user_id = due_entries.user_id
   and device.enabled is true
  where due_entries.due_at is not null
    and due_entries.due_at <= now()
    and due_entries.due_at > now() - interval '24 hours'
    and not exists (
      select 1
      from public.breeze_push_reminder_deliveries as delivery
      where delivery.user_id = due_entries.user_id
        and delivery.reminder_id = due_entries.reminder_id
        and delivery.due_at = due_entries.due_at
        and delivery.installation_id = device.installation_id
        and (delivery.sent_at is not null or delivery.claim_until > now())
    )
  order by due_entries.due_at asc
  limit 100;
$$;

create or replace function public.claim_breeze_reminder_push_delivery(
  p_user_id uuid,
  p_reminder_id text,
  p_due_at timestamptz,
  p_installation_id text
)
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
  did_claim boolean := false;
begin
  insert into public.breeze_push_reminder_deliveries (
    user_id, reminder_id, due_at, installation_id, claim_until, attempts
  ) values (
    p_user_id, p_reminder_id, p_due_at, p_installation_id, now() + interval '5 minutes', 1
  )
  on conflict (user_id, reminder_id, due_at, installation_id) do update
    set claim_until = now() + interval '5 minutes',
        attempts = public.breeze_push_reminder_deliveries.attempts + 1
    where public.breeze_push_reminder_deliveries.sent_at is null
      and public.breeze_push_reminder_deliveries.claim_until <= now()
  returning true into did_claim;

  return coalesce(did_claim, false);
end;
$$;

create or replace function public.complete_breeze_reminder_push_delivery(
  p_user_id uuid,
  p_reminder_id text,
  p_due_at timestamptz,
  p_installation_id text
)
returns void
language sql
security definer
set search_path = ''
as $$
  update public.breeze_push_reminder_deliveries
     set sent_at = now()
   where user_id = p_user_id
     and reminder_id = p_reminder_id
     and due_at = p_due_at
     and installation_id = p_installation_id
     and sent_at is null;
$$;

create or replace function public.release_breeze_reminder_push_delivery(
  p_user_id uuid,
  p_reminder_id text,
  p_due_at timestamptz,
  p_installation_id text
)
returns void
language sql
security definer
set search_path = ''
as $$
  update public.breeze_push_reminder_deliveries
     set claim_until = now() - interval '1 second'
   where user_id = p_user_id
     and reminder_id = p_reminder_id
     and due_at = p_due_at
     and installation_id = p_installation_id
     and sent_at is null;
$$;

create or replace function public.is_breeze_reminder_scheduler_token_valid(p_token text)
returns boolean
language sql
security definer
set search_path = ''
stable
as $$
  select p_token is not null
     and length(p_token) between 32 and 256
     and exists (
       select 1
       from vault.decrypted_secrets as secret
       where secret.name = 'breeze_reminder_scheduler_token'
         and secret.decrypted_secret = p_token
     );
$$;

do $$
declare
  scheduler_token text;
begin
  if not exists (
    select 1
    from vault.secrets
    where name = 'breeze_reminder_scheduler_token'
  ) then
    scheduler_token := replace(pg_catalog.gen_random_uuid()::text, '-', '')
      || replace(pg_catalog.gen_random_uuid()::text, '-', '');
    perform vault.create_secret(
      scheduler_token,
      'breeze_reminder_scheduler_token',
      'Private bearer token used only by the Breeze reminder cron job.'
    );
  end if;
end;
$$;

revoke all on function public.get_breeze_due_reminder_push_targets() from public, anon, authenticated;
revoke all on function public.claim_breeze_reminder_push_delivery(uuid, text, timestamptz, text) from public, anon, authenticated;
revoke all on function public.complete_breeze_reminder_push_delivery(uuid, text, timestamptz, text) from public, anon, authenticated;
revoke all on function public.release_breeze_reminder_push_delivery(uuid, text, timestamptz, text) from public, anon, authenticated;
revoke all on function public.is_breeze_reminder_scheduler_token_valid(text) from public, anon, authenticated;
grant execute on function public.get_breeze_due_reminder_push_targets() to service_role;
grant execute on function public.claim_breeze_reminder_push_delivery(uuid, text, timestamptz, text) to service_role;
grant execute on function public.complete_breeze_reminder_push_delivery(uuid, text, timestamptz, text) to service_role;
grant execute on function public.release_breeze_reminder_push_delivery(uuid, text, timestamptz, text) to service_role;
grant execute on function public.is_breeze_reminder_scheduler_token_valid(text) to service_role;

select cron.unschedule(jobid)
from cron.job
where jobname in ('breeze-reminder-push-dispatch', 'breeze-reminder-push-delivery-cleanup');

select cron.schedule(
  'breeze-reminder-push-dispatch',
  '* * * * *',
  $job$
    select net.http_post(
      url := 'https://sbvjjseitpahdpewsqqc.supabase.co/functions/v1/deliver-reminder-pushes',
      headers := jsonb_build_object(
        'Content-Type', 'application/json',
        'Authorization', 'Bearer ' || secret.decrypted_secret
      ),
      body := '{}'::jsonb,
      timeout_milliseconds := 10000
    )
    from vault.decrypted_secrets as secret
    where secret.name = 'breeze_reminder_scheduler_token'
    limit 1;
  $job$
);

select cron.schedule(
  'breeze-reminder-push-delivery-cleanup',
  '15 4 * * *',
  $$delete from public.breeze_push_reminder_deliveries where due_at < now() - interval '90 days';$$
);
