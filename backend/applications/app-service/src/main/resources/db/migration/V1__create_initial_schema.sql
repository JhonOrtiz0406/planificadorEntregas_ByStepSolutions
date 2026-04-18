-- ========================================
-- V1: Initial schema for DeliveryPlanner
-- ByStep Solutions S.A.S.
-- ========================================

-- Enable UUID extension
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ========================================
-- ORGANIZATIONS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) UNIQUE NOT NULL,
    logo_url    TEXT,
    admin_email VARCHAR(255) NOT NULL,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_organizations_slug ON organizations(slug);
CREATE INDEX IF NOT EXISTS idx_organizations_admin_email ON organizations(admin_email);

-- ========================================
-- USERS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_id       VARCHAR(255) UNIQUE,
    email           VARCHAR(255) UNIQUE NOT NULL,
    name            VARCHAR(255),
    picture_url     TEXT,
    role            VARCHAR(50) NOT NULL
                        CHECK (role IN ('PLATFORM_ADMIN', 'ORG_ADMIN', 'ORG_EMPLOYEE', 'ORG_DELIVERY')),
    organization_id UUID REFERENCES organizations(id) ON DELETE SET NULL,
    fcm_token       TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_google_id ON users(google_id);
CREATE INDEX IF NOT EXISTS idx_users_organization_id ON users(organization_id);

-- ========================================
-- ORDERS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS orders (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_number    VARCHAR(50) NOT NULL,
    product_name    VARCHAR(255) NOT NULL,
    client_name     VARCHAR(255) NOT NULL,
    client_phone    VARCHAR(50),
    client_address  TEXT,
    description     TEXT,
    photo_url       TEXT,
    delivery_date   DATE NOT NULL,
    progress_status VARCHAR(50) DEFAULT 'NOT_STARTED'
                        CHECK (progress_status IN ('NOT_STARTED', 'IN_PREPARATION', 'DELIVERED')),
    payment_status  VARCHAR(50) DEFAULT 'UNPAID'
                        CHECK (payment_status IN ('UNPAID', 'PARTIAL', 'PAID')),
    payment_amount  NUMERIC(12, 2) DEFAULT 0,
    total_price     NUMERIC(12, 2),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    created_by      UUID REFERENCES users(id) ON DELETE SET NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_orders_organization_id ON orders(organization_id);
CREATE INDEX IF NOT EXISTS idx_orders_delivery_date ON orders(delivery_date);
CREATE INDEX IF NOT EXISTS idx_orders_progress_status ON orders(progress_status);
CREATE INDEX IF NOT EXISTS idx_orders_org_delivery ON orders(organization_id, delivery_date);

-- ========================================
-- INVITATIONS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS invitations (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL,
    role            VARCHAR(50) NOT NULL
                        CHECK (role IN ('PLATFORM_ADMIN', 'ORG_ADMIN', 'ORG_EMPLOYEE', 'ORG_DELIVERY')),
    organization_id UUID NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    token           VARCHAR(255) UNIQUE NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    accepted        BOOLEAN DEFAULT FALSE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_invitations_token ON invitations(token);
CREATE INDEX IF NOT EXISTS idx_invitations_email ON invitations(email);

-- ========================================
-- REMINDERS TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS reminders (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id      UUID NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    reminder_date DATE NOT NULL,
    days_before   INTEGER,
    sent          BOOLEAN DEFAULT FALSE,
    sent_at       TIMESTAMPTZ,
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_reminders_order_id ON reminders(order_id);
CREATE INDEX IF NOT EXISTS idx_reminders_date_sent ON reminders(reminder_date, sent);
