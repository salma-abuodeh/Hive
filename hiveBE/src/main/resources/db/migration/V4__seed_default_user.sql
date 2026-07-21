-- Shared bcrypt hash for all default users (password: password123)
-- Generated with Spring BCryptPasswordEncoder ($2a$)
-- $2a$10$0RCcyNURbY2NvT6KLxMbD.3tFLsMkAEoztnHuu3nqbLb58q9SEcIK

-- Demo company for manager / employee memberships
INSERT INTO companies (name, company_type, domain, status, active)
SELECT 'Demo Company', 'COMPANY', 'demo.hive.local', 'active', true
WHERE NOT EXISTS (
    SELECT 1 FROM companies c WHERE c.domain = 'demo.hive.local'
);

-- Platform Administrator
INSERT INTO users (platform_role_id, first_name, last_name, email, password_hash, job_title, status, active)
SELECT r.id,
       'Hive',
       'Admin',
       'admin@hive.local',
       '$2a$10$0RCcyNURbY2NvT6KLxMbD.3tFLsMkAEoztnHuu3nqbLb58q9SEcIK',
       'Platform Administrator',
       'active',
       true
FROM roles r
WHERE r.name = 'Platform Administrator'
  AND r.company_id IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM users u WHERE u.email = 'admin@hive.local'
  );

-- Manager (company-scoped)
INSERT INTO users (first_name, last_name, email, password_hash, job_title, status, active)
SELECT 'Demo',
       'Manager',
       'manager@hive.local',
       '$2a$10$0RCcyNURbY2NvT6KLxMbD.3tFLsMkAEoztnHuu3nqbLb58q9SEcIK',
       'Manager',
       'active',
       true
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.email = 'manager@hive.local'
);

-- Employee (company-scoped)
INSERT INTO users (first_name, last_name, email, password_hash, job_title, status, active)
SELECT 'Demo',
       'Employee',
       'employee@hive.local',
       '$2a$10$0RCcyNURbY2NvT6KLxMbD.3tFLsMkAEoztnHuu3nqbLb58q9SEcIK',
       'Employee',
       'active',
       true
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.email = 'employee@hive.local'
);

-- Attach manager + employee to Demo Company with the matching global roles
INSERT INTO user_companies (user_id, company_id, role_id, active)
SELECT u.id, c.id, r.id, true
FROM users u
JOIN companies c ON c.domain = 'demo.hive.local'
JOIN roles r ON r.name = 'Manager' AND r.company_id IS NULL
WHERE u.email = 'manager@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_companies uc
      WHERE uc.user_id = u.id AND uc.company_id = c.id
  );

INSERT INTO user_companies (user_id, company_id, role_id, active)
SELECT u.id, c.id, r.id, true
FROM users u
JOIN companies c ON c.domain = 'demo.hive.local'
JOIN roles r ON r.name = 'Employee' AND r.company_id IS NULL
WHERE u.email = 'employee@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_companies uc
      WHERE uc.user_id = u.id AND uc.company_id = c.id
  );
