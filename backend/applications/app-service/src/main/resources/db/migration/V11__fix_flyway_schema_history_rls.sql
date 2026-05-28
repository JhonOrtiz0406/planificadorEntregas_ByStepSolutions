-- ========================================
-- V11: Fix flyway_schema_history — disable RLS, revoke API grants
-- Internal Flyway table must not be exposed via PostgREST.
-- RLS was enabled (from V5 blanket enable) but no policies existed,
-- causing a Supabase security advisor warning (deny-all implicit).
-- Flyway runs as service_role which bypasses RLS — no functional impact.
-- ByStep Solutions S.A.S.
-- ========================================

ALTER TABLE public.flyway_schema_history DISABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.flyway_schema_history FROM anon, authenticated;
