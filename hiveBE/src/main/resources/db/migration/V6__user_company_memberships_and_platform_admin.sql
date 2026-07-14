CREATE TABLE user_company_memberships (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    company_id  BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uq_user_company UNIQUE (user_id, company_id)
);

CREATE INDEX idx_memberships_user ON user_company_memberships(user_id);
CREATE INDEX idx_memberships_company ON user_company_memberships(company_id);

INSERT INTO user_company_memberships (user_id, company_id, created_at)
SELECT u.id, u.company_id, COALESCE(u.created_at, now())
FROM users u
WHERE u.company_id IS NOT NULL
ON CONFLICT DO NOTHING;

ALTER TABLE users DROP COLUMN company_id;

INSERT INTO roles (name, description, company_id)
SELECT 'Platform Admin', 'Internal platform administrator', NULL
WHERE NOT EXISTS (
    SELECT 1 FROM roles WHERE name = 'Platform Admin' AND company_id IS NULL
);

-- Password = Password123!  (bcrypt)
INSERT INTO users (first_name, last_name, email, password, created_at, role_id, active)
SELECT
    'Platform',
    'Admin',
    'platform@hive.local',
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQubh4a',
    now(),
    r.id,
    true
FROM roles r
WHERE r.name = 'Platform Admin'
  AND r.company_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'platform@hive.local');