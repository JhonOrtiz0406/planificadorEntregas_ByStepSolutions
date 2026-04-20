CREATE TABLE user_organizations (
    user_id         UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    organization_id UUID        NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    role            VARCHAR(50) NOT NULL,
    joined_at       TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (user_id, organization_id)
);

CREATE INDEX idx_user_orgs_user_id ON user_organizations(user_id);

-- Backfill from existing users that already belong to an org
INSERT INTO user_organizations (user_id, organization_id, role)
SELECT id, organization_id, role::text
FROM users
WHERE organization_id IS NOT NULL
ON CONFLICT DO NOTHING;
