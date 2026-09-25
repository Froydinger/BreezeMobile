create table if not exists public.breeze_push_devices (
  user_id uuid not null references auth.users (id) on delete cascade,
  installation_id text not null check (installation_id ~ '^[A-Za-z0-9_-]{20,64}$'),
  enabled boolean not null default true,
  updated_at timestamptz not null default now(),
  primary key (user_id, installation_id)
);

alter table public.breeze_push_devices enable row level security;
revoke all on public.breeze_push_devices from public, anon;
grant select, insert, update, delete on public.breeze_push_devices to authenticated;

drop policy if exists breeze_push_devices_read_own on public.breeze_push_devices;
create policy breeze_push_devices_read_own on public.breeze_push_devices
  for select to authenticated using ((select auth.uid()) = user_id);
drop policy if exists breeze_push_devices_insert_own on public.breeze_push_devices;
create policy breeze_push_devices_insert_own on public.breeze_push_devices
  for insert to authenticated with check ((select auth.uid()) = user_id);
drop policy if exists breeze_push_devices_update_own on public.breeze_push_devices;
create policy breeze_push_devices_update_own on public.breeze_push_devices
  for update to authenticated using ((select auth.uid()) = user_id)
  with check ((select auth.uid()) = user_id);
drop policy if exists breeze_push_devices_delete_own on public.breeze_push_devices;
create policy breeze_push_devices_delete_own on public.breeze_push_devices
  for delete to authenticated using ((select auth.uid()) = user_id);

drop trigger if exists breeze_push_devices_touch on public.breeze_push_devices;
create trigger breeze_push_devices_touch before update on public.breeze_push_devices
  for each row execute function public.touch_breeze_sync_updated_at();
