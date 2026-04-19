-- ========================================
-- V5: Row-Level Security for tenant isolation
-- Protects all tenant tables against cross-org data access
-- from direct DB/Supabase API queries (non-superuser roles).
-- The Spring Boot app uses the postgres/service_role which
-- bypasses RLS by default. These policies guard against:
--   - Supabase PostgREST direct API access
--   - Read-only reporting users
--   - Dashboard queries from non-admin users
-- ByStep Solutions S.A.S.
-- ========================================

-- ── orders ──────────────────────────────────────────────────
ALTER TABLE orders ENABLE ROW LEVEL SECURITY;

-- Deny all access to anon role (unauthenticated Supabase)
CREATE POLICY orders_deny_anon ON orders
    AS RESTRICTIVE
    TO anon
    USING (false);

-- authenticated role may only see rows for their organization
-- (JWT claim app_metadata.organization_id set by backend)
CREATE POLICY orders_org_isolation ON orders
    AS PERMISSIVE
    FOR ALL
    TO authenticated
    USING (organization_id::text = current_setting('request.jwt.claims', true)::json->>'organization_id');

-- ── reminders ───────────────────────────────────────────────
ALTER TABLE reminders ENABLE ROW LEVEL SECURITY;

CREATE POLICY reminders_deny_anon ON reminders
    AS RESTRICTIVE
    TO anon
    USING (false);

CREATE POLICY reminders_org_isolation ON reminders
    AS PERMISSIVE
    FOR ALL
    TO authenticated
    USING (organization_id::text = current_setting('request.jwt.claims', true)::json->>'organization_id');

-- ── invitations ──────────────────────────────────────────────
ALTER TABLE invitations ENABLE ROW LEVEL SECURITY;

CREATE POLICY invitations_deny_anon ON invitations
    AS RESTRICTIVE
    TO anon
    USING (false);

CREATE POLICY invitations_org_isolation ON invitations
    AS PERMISSIVE
    FOR ALL
    TO authenticated
    USING (organization_id::text = current_setting('request.jwt.claims', true)::json->>'organization_id');

-- ── users ────────────────────────────────────────────────────
ALTER TABLE users ENABLE ROW LEVEL SECURITY;

CREATE POLICY users_deny_anon ON users
    AS RESTRICTIVE
    TO anon
    USING (false);

CREATE POLICY users_org_isolation ON users
    AS PERMISSIVE
    FOR ALL
    TO authenticated
    USING (
        organization_id IS NULL
        OR organization_id::text = current_setting('request.jwt.claims', true)::json->>'organization_id'
    );
