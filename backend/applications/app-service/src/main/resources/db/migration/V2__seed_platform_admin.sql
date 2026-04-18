-- ========================================
-- V2: Seed initial platform admin
-- Update email below before deployment
-- ========================================

INSERT INTO users (id, email, name, role, is_active, created_at, updated_at)
VALUES (
    gen_random_uuid(),
    '${PLATFORM_ADMIN_EMAIL}',
    'Platform Administrator',
    'PLATFORM_ADMIN',
    TRUE,
    NOW(),
    NOW()
) ON CONFLICT (email) DO NOTHING;
