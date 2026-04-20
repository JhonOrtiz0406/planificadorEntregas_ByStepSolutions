-- ========================================
-- V4: Add organization_id to reminders for direct tenant isolation
-- ByStep Solutions S.A.S.
-- ========================================

ALTER TABLE reminders
    ADD COLUMN IF NOT EXISTS organization_id UUID REFERENCES organizations(id) ON DELETE CASCADE;

-- Backfill from parent order
UPDATE reminders r
SET organization_id = o.organization_id
FROM orders o
WHERE r.order_id = o.id
  AND r.organization_id IS NULL;

-- Enforce NOT NULL after backfill
ALTER TABLE reminders
    ALTER COLUMN organization_id SET NOT NULL;

-- Composite index for efficient org + date queries
CREATE INDEX IF NOT EXISTS idx_reminders_org_date ON reminders(organization_id, reminder_date);
