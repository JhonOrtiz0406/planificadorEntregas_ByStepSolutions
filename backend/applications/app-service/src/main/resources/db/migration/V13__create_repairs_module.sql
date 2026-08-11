-- ========================================
-- V13: "Arreglos" module (jewelry repair tracking)
-- Only used by organizations of category JEWELRY (enforced in app layer).
-- ByStep Solutions S.A.S.
-- ========================================

-- ========================================
-- REPAIRS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS repairs (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_first_name  VARCHAR(255) NOT NULL,
    client_last_name   VARCHAR(255) NOT NULL,
    client_phone       VARCHAR(50)  NOT NULL,
    item_description   TEXT NOT NULL,
    repair_description TEXT NOT NULL,
    photo_urls         TEXT,
    entry_date         DATE NOT NULL DEFAULT CURRENT_DATE,
    delivery_date      DATE,
    repair_status      VARCHAR(50) NOT NULL DEFAULT 'RECEIVED'
                           CHECK (repair_status IN ('RECEIVED', 'IN_PROGRESS', 'READY_TO_DELIVER', 'DELIVERED')),
    payment_status     VARCHAR(50) NOT NULL DEFAULT 'UNPAID'
                           CHECK (payment_status IN ('UNPAID', 'PARTIAL', 'PAID')),
    total_price        NUMERIC(15, 2) NOT NULL,
    payment_amount     NUMERIC(15, 2) DEFAULT 0,
    organization_id    UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_by         UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at         TIMESTAMPTZ DEFAULT NOW(),
    updated_at         TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_repairs_organization_id ON repairs(organization_id);
CREATE INDEX IF NOT EXISTS idx_repairs_entry_date ON repairs(entry_date);
CREATE INDEX IF NOT EXISTS idx_repairs_repair_status ON repairs(repair_status);
CREATE INDEX IF NOT EXISTS idx_repairs_org_entry ON repairs(organization_id, entry_date);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.repairs TO anon, authenticated;

-- ========================================
-- REPAIR_PAYMENTS TABLE — abonos history
-- ========================================
CREATE TABLE IF NOT EXISTS repair_payments (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repair_id      UUID NOT NULL REFERENCES repairs(id) ON DELETE CASCADE,
    amount         NUMERIC(15, 2) NOT NULL,
    payment_date   DATE NOT NULL,
    payment_method VARCHAR(100),
    notes          TEXT,
    created_by     UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at     TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_repair_payments_repair_id ON repair_payments(repair_id);

GRANT SELECT, INSERT, UPDATE, DELETE ON TABLE public.repair_payments TO anon, authenticated;

-- ========================================
-- Row Level Security — tenant isolation (mirrors V5/V12)
-- ========================================
ALTER TABLE repairs ENABLE ROW LEVEL SECURITY;

CREATE POLICY repairs_deny_anon ON repairs
    AS RESTRICTIVE
    TO anon
    USING (false);

CREATE POLICY repairs_org_isolation ON repairs
    AS PERMISSIVE
    FOR ALL
    TO authenticated
    USING (organization_id::text = current_setting('request.jwt.claims', true)::json->>'organization_id');

ALTER TABLE repair_payments ENABLE ROW LEVEL SECURITY;

CREATE POLICY repair_payments_deny_anon ON repair_payments
    AS RESTRICTIVE
    TO anon
    USING (false);

CREATE POLICY repair_payments_org_isolation ON repair_payments
    AS PERMISSIVE
    FOR ALL
    TO authenticated
    USING (
        repair_id IN (
            SELECT id FROM repairs
            WHERE organization_id::text = current_setting('request.jwt.claims', true)::json->>'organization_id'
        )
    );
