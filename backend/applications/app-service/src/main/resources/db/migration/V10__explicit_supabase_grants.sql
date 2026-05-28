-- ========================================
-- V10: Explicit Supabase API grants (Oct 2026 compatibility)
-- From 2026-10-30, new tables in existing Supabase projects will NOT
-- be exposed to the PostgREST API by default — explicit GRANTs required.
-- This migration locks in grants on all current tables so behavior
-- is preserved after the policy change, and serves as the template
-- for every new table going forward.
-- RLS policies (V5) still enforce row-level tenant isolation.
-- ByStep Solutions S.A.S.
-- ========================================

-- ── Application tables ────────────────────────────────────────
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.organizations       TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.users               TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.orders              TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.invitations         TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.reminders           TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.org_categories      TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.category_statuses   TO anon, authenticated;
GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.user_organizations  TO anon, authenticated;

-- ── Template for new tables ───────────────────────────────────
-- When adding a new table, append at the end of its CREATE TABLE block:
--
--   GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.<new_table> TO anon, authenticated;
--
-- Required from 2026-10-30: Supabase no longer auto-exposes new tables.
-- Without this GRANT the table will be invisible to PostgREST / the client SDK.
-- ========================================
