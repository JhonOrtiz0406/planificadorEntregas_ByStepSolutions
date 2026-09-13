-- ========================================
-- V14: Datos del administrador, membresías por organización y
--      notificaciones WhatsApp multi-tenant.
-- Migración 100% ADITIVA: no borra ni renombra nada existente.
-- ByStep Solutions S.A.S.
-- ========================================

-- ── 1. Datos del administrador y número de la organización ─────────────
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS admin_first_name   VARCHAR(100);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS admin_last_name    VARCHAR(100);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS admin_phone        VARCHAR(20);
ALTER TABLE organizations ADD COLUMN IF NOT EXISTS organization_phone VARCHAR(20);

-- ── 2. Estado de la membresía POR organización ────────────────────────
-- Inhabilitar a alguien en la organización A ya no afecta su acceso a B.
ALTER TABLE user_organizations ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

-- Backfill: los usuarios hoy inhabilitados quedan con la membresía de su
-- organización actual inhabilitada (mismo comportamiento que antes).
UPDATE user_organizations uo
SET is_active = FALSE
FROM users u
WHERE u.id = uo.user_id
  AND u.organization_id = uo.organization_id
  AND u.is_active = FALSE;

CREATE INDEX IF NOT EXISTS idx_user_orgs_org_id ON user_organizations(organization_id);

-- ── 3. Consentimiento de notificación por pedido / arreglo ─────────────
ALTER TABLE orders  ADD COLUMN IF NOT EXISTS notify_whatsapp BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE repairs ADD COLUMN IF NOT EXISTS notify_whatsapp BOOLEAN NOT NULL DEFAULT TRUE;

-- ── 4. Configuración WhatsApp por organización ────────────────────────
-- Cada organización tiene su propio WABA, número y token (cifrado).
CREATE TABLE IF NOT EXISTS organization_whatsapp_config (
    organization_id      UUID PRIMARY KEY REFERENCES organizations(id) ON DELETE CASCADE,
    enabled              BOOLEAN      NOT NULL DEFAULT FALSE,
    status               VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
                             CHECK (status IN ('PENDING', 'CONNECTED', 'ERROR')),
    waba_id              VARCHAR(50),
    phone_number_id      VARCHAR(50) UNIQUE,
    display_phone_number VARCHAR(30),
    verified_name        VARCHAR(255),
    access_token_enc     TEXT,
    graph_api_version    VARCHAR(10),
    language_code        VARCHAR(10)  NOT NULL DEFAULT 'es',
    support_contact_text VARCHAR(60),
    quality_rating       VARCHAR(20),
    last_error           TEXT,
    last_verified_at     TIMESTAMPTZ,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at           TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Estado de cada plantilla del catálogo en el WABA de cada organización
CREATE TABLE IF NOT EXISTS organization_whatsapp_templates (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id    UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    template_key       VARCHAR(60)  NOT NULL,
    template_name      VARCHAR(100) NOT NULL,
    language_code      VARCHAR(10)  NOT NULL DEFAULT 'es',
    meta_template_id   VARCHAR(50),
    status             VARCHAR(20)  NOT NULL DEFAULT 'NOT_CREATED',
    rejection_reason   TEXT,
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    UNIQUE (organization_id, template_key, language_code)
);

-- Eventos que cada organización quiere notificar
CREATE TABLE IF NOT EXISTS organization_notification_settings (
    organization_id  UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    event_key        VARCHAR(60) NOT NULL,
    whatsapp_enabled BOOLEAN     NOT NULL DEFAULT TRUE,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (organization_id, event_key)
);

-- Cola de salida (outbox) + historial de mensajes, siempre por organización
CREATE TABLE IF NOT EXISTS whatsapp_messages (
    id               UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id  UUID         NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    event_key        VARCHAR(60)  NOT NULL,
    entity_type      VARCHAR(20),
    entity_id        UUID,
    to_phone         VARCHAR(20),
    template_name    VARCHAR(100),
    language_code    VARCHAR(10),
    params           TEXT,
    status           VARCHAR(20)  NOT NULL
                         CHECK (status IN ('PENDING', 'SENDING', 'SENT', 'FAILED', 'SKIPPED')),
    skip_reason      VARCHAR(60),
    wamid            VARCHAR(128),
    error_code       VARCHAR(20),
    error_message    TEXT,
    attempts         INTEGER      NOT NULL DEFAULT 0,
    next_attempt_at  TIMESTAMPTZ,
    idempotency_key  VARCHAR(200) UNIQUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    sent_at          TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_wa_messages_due    ON whatsapp_messages(status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_wa_messages_org    ON whatsapp_messages(organization_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_wa_messages_entity ON whatsapp_messages(entity_type, entity_id);

-- ── 5. Seguridad ──────────────────────────────────────────────────────
-- Estas tablas SOLO las usa el backend (rol de servicio, que omite RLS).
-- Contienen tokens cifrados y teléfonos: NO se exponen por PostgREST.
REVOKE ALL ON TABLE public.organization_whatsapp_config       FROM anon, authenticated;
REVOKE ALL ON TABLE public.organization_whatsapp_templates    FROM anon, authenticated;
REVOKE ALL ON TABLE public.organization_notification_settings FROM anon, authenticated;
REVOKE ALL ON TABLE public.whatsapp_messages                  FROM anon, authenticated;

ALTER TABLE organization_whatsapp_config       ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_whatsapp_templates    ENABLE ROW LEVEL SECURITY;
ALTER TABLE organization_notification_settings ENABLE ROW LEVEL SECURITY;
ALTER TABLE whatsapp_messages                  ENABLE ROW LEVEL SECURITY;

CREATE POLICY wa_config_deny_all ON organization_whatsapp_config
    AS RESTRICTIVE TO anon, authenticated USING (false);
CREATE POLICY wa_templates_deny_all ON organization_whatsapp_templates
    AS RESTRICTIVE TO anon, authenticated USING (false);
CREATE POLICY notif_settings_deny_all ON organization_notification_settings
    AS RESTRICTIVE TO anon, authenticated USING (false);
CREATE POLICY wa_messages_deny_all ON whatsapp_messages
    AS RESTRICTIVE TO anon, authenticated USING (false);
