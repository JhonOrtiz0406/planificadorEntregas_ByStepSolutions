-- ── payment_records ─────────────────────────────────────────
ALTER TABLE payment_records ENABLE ROW LEVEL SECURITY;

-- Deny all access to anon role (unauthenticated Supabase)
CREATE POLICY payment_records_deny_anon ON payment_records
    AS RESTRICTIVE
    TO anon
    USING (false);

-- authenticated role: solo pagos de órdenes de su organización
CREATE POLICY payment_records_org_isolation ON payment_records
    AS PERMISSIVE
    FOR ALL
    TO authenticated
    USING (
    order_id IN (
        SELECT id FROM orders
        WHERE organization_id::text = current_setting('request.jwt.claims', true)::json->>'organization_id'
    )
    );