-- ========================================
-- V6: Organization categories + dynamic order statuses
-- ByStep Solutions S.A.S.
-- ========================================

-- ── Categories master table ───────────────────────────────────
CREATE TABLE IF NOT EXISTS org_categories (
    id    VARCHAR(100) PRIMARY KEY,
    label VARCHAR(255) NOT NULL
);

INSERT INTO org_categories (id, label) VALUES
    ('GENERAL', 'General'),
    ('JEWELRY',  'Joyería'),
    ('LAUNDRY',  'Lavandería'),
    ('PHONES',   'Celulares');

-- ── Status definitions per category ──────────────────────────
CREATE TABLE IF NOT EXISTS category_statuses (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id   VARCHAR(100) NOT NULL REFERENCES org_categories(id) ON DELETE CASCADE,
    status_key    VARCHAR(100) NOT NULL,
    label         VARCHAR(255) NOT NULL,
    display_order INTEGER      NOT NULL,
    is_final      BOOLEAN      NOT NULL DEFAULT FALSE,
    UNIQUE (category_id, status_key),
    UNIQUE (category_id, display_order)
);

-- GENERAL (backward-compatible keys)
INSERT INTO category_statuses (category_id, status_key, label, display_order, is_final) VALUES
    ('GENERAL', 'NOT_STARTED',    'Sin iniciar',      1, false),
    ('GENERAL', 'IN_PREPARATION', 'En preparación',   2, false),
    ('GENERAL', 'DELIVERED',      'Entregado',        3, true);

-- JEWELRY
INSERT INTO category_statuses (category_id, status_key, label, display_order, is_final) VALUES
    ('JEWELRY', 'NOT_STARTED',      'Sin iniciar',        1, false),
    ('JEWELRY', 'DESIGN',           'Mandar a diseñar',   2, false),
    ('JEWELRY', 'PRINT',            'Imprimir',           3, false),
    ('JEWELRY', 'EMPTY',            'Vaciar',             4, false),
    ('JEWELRY', 'POLISH',           'Pulir',              5, false),
    ('JEWELRY', 'READY_TO_DELIVER', 'Por entregar',       6, false),
    ('JEWELRY', 'DELIVERED',        'Entregado',          7, true);

-- ── Add category to organizations ─────────────────────────────
ALTER TABLE organizations
    ADD COLUMN IF NOT EXISTS category VARCHAR(100) NOT NULL DEFAULT 'GENERAL'
    REFERENCES org_categories(id);

-- ── Remove old CHECK constraint and allow any status key ──────
ALTER TABLE orders DROP CONSTRAINT IF EXISTS orders_progress_status_check;
