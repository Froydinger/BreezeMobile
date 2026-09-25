create table if not exists public.breeze_sync_preferences (
  user_id uuid primary key references auth.users (id) on delete cascade,
  bookmarks boolean not null default false,
  tabs boolean not null default false,
  history boolean not null default false,
  chats boolean not null default false,
  reminders boolean not null default false,
  updated_at timestamptz not null default now()
);

create table if not exists public.breeze_sync_collections (
  user_id uuid not null references auth.users (id) on delete cascade,
  collection text not null check (collection in ('bookmarks', 'tabs', 'history', 'chats', 'reminders')),
  payload jsonb not null default '{"formatVersion":1,"entries":[],"deletedIds":[]}'::jsonb,
  updated_at timestamptz not null default now(),
  primary key (user_id, collection)
);

alter table public.breeze_sync_preferences enable row level security;
alter table public.breeze_sync_collections enable row level security;

revoke all on public.breeze_sync_preferences from public, anon;
revoke all on public.breeze_sync_collections from public, anon;
grant select, insert, update, delete on public.breeze_sync_preferences to authenticated;
grant select, insert, update, delete on public.breeze_sync_collections to authenticated;

drop policy if exists breeze_sync_preferences_read_own on public.breeze_sync_preferences;
create policy breeze_sync_preferences_read_own on public.breeze_sync_preferences
  for select to authenticated using ((select auth.uid()) = user_id);
drop policy if exists breeze_sync_preferences_insert_own on public.breeze_sync_preferences;
create policy breeze_sync_preferences_insert_own on public.breeze_sync_preferences
  for insert to authenticated with check ((select auth.uid()) = user_id);
drop policy if exists breeze_sync_preferences_update_own on public.breeze_sync_preferences;
create policy breeze_sync_preferences_update_own on public.breeze_sync_preferences
  for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
drop policy if exists breeze_sync_preferences_delete_own on public.breeze_sync_preferences;
create policy breeze_sync_preferences_delete_own on public.breeze_sync_preferences
  for delete to authenticated using ((select auth.uid()) = user_id);

drop policy if exists breeze_sync_collections_read_own on public.breeze_sync_collections;
create policy breeze_sync_collections_read_own on public.breeze_sync_collections
  for select to authenticated using ((select auth.uid()) = user_id);
drop policy if exists breeze_sync_collections_insert_own on public.breeze_sync_collections;
create policy breeze_sync_collections_insert_own on public.breeze_sync_collections
  for insert to authenticated with check ((select auth.uid()) = user_id);
drop policy if exists breeze_sync_collections_update_own on public.breeze_sync_collections;
create policy breeze_sync_collections_update_own on public.breeze_sync_collections
  for update to authenticated using ((select auth.uid()) = user_id) with check ((select auth.uid()) = user_id);
drop policy if exists breeze_sync_collections_delete_own on public.breeze_sync_collections;
create policy breeze_sync_collections_delete_own on public.breeze_sync_collections
  for delete to authenticated using ((select auth.uid()) = user_id);

create or replace function public.touch_breeze_sync_updated_at()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists breeze_sync_preferences_touch on public.breeze_sync_preferences;
create trigger breeze_sync_preferences_touch before update on public.breeze_sync_preferences
  for each row execute function public.touch_breeze_sync_updated_at();
drop trigger if exists breeze_sync_collections_touch on public.breeze_sync_collections;
create trigger breeze_sync_collections_touch before update on public.breeze_sync_collections
  for each row execute function public.touch_breeze_sync_updated_at();
