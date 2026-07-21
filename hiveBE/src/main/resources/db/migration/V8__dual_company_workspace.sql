-- Second company + dual-membership demo account (password: password123)
-- dual@hive.local is Manager in Demo Company and Employee in Antonian Care Co.
-- Different teams in each company so workspace switching is visible.

INSERT INTO companies (name, company_type, domain, status, active)
SELECT 'Antonian Care Co.', 'COMPANY', 'antonian.hive.local', 'active', true
WHERE NOT EXISTS (
    SELECT 1 FROM companies c WHERE c.domain = 'antonian.hive.local'
);

INSERT INTO users (first_name, last_name, email, password_hash, job_title, status, active)
SELECT 'Alex',
       'Rivera',
       'dual@hive.local',
       '$2a$10$0RCcyNURbY2NvT6KLxMbD.3tFLsMkAEoztnHuu3nqbLb58q9SEcIK',
       'Cross-company Member',
       'active',
       true
WHERE NOT EXISTS (
    SELECT 1 FROM users u WHERE u.email = 'dual@hive.local'
);

-- Demo Company: dual is Manager
INSERT INTO user_companies (user_id, company_id, role_id, active)
SELECT u.id, c.id, r.id, true
FROM users u
JOIN companies c ON c.domain = 'demo.hive.local'
JOIN roles r ON r.name = 'Manager' AND r.company_id IS NULL
WHERE u.email = 'dual@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_companies uc
      WHERE uc.user_id = u.id AND uc.company_id = c.id
  );

-- Antonian Care Co.: dual is Employee
INSERT INTO user_companies (user_id, company_id, role_id, active)
SELECT u.id, c.id, r.id, true
FROM users u
JOIN companies c ON c.domain = 'antonian.hive.local'
JOIN roles r ON r.name = 'Employee' AND r.company_id IS NULL
WHERE u.email = 'dual@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_companies uc
      WHERE uc.user_id = u.id AND uc.company_id = c.id
  );

-- Also put Demo Manager into Antonian as Manager so that workspace has an admin
INSERT INTO user_companies (user_id, company_id, role_id, active)
SELECT u.id, c.id, r.id, true
FROM users u
JOIN companies c ON c.domain = 'antonian.hive.local'
JOIN roles r ON r.name = 'Manager' AND r.company_id IS NULL
WHERE u.email = 'manager@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_companies uc
      WHERE uc.user_id = u.id AND uc.company_id = c.id
  );

-- Teams in Demo Company
INSERT INTO teams (company_id, name, description, active)
SELECT c.id, 'Engineering', 'Product and engineering', true
FROM companies c
WHERE c.domain = 'demo.hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM teams t WHERE t.company_id = c.id AND t.name = 'Engineering'
  );

INSERT INTO teams (company_id, name, description, active)
SELECT c.id, 'Operations', 'Company operations', true
FROM companies c
WHERE c.domain = 'demo.hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM teams t WHERE t.company_id = c.id AND t.name = 'Operations'
  );

-- Teams in Antonian Care Co.
INSERT INTO teams (company_id, name, description, active)
SELECT c.id, 'Care Ops', 'Front-line care operations', true
FROM companies c
WHERE c.domain = 'antonian.hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM teams t WHERE t.company_id = c.id AND t.name = 'Care Ops'
  );

INSERT INTO teams (company_id, name, description, active)
SELECT c.id, 'Clinical', 'Clinical specialists', true
FROM companies c
WHERE c.domain = 'antonian.hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM teams t WHERE t.company_id = c.id AND t.name = 'Clinical'
  );

-- dual on Engineering (Demo) and Care Ops (Antonian)
INSERT INTO user_teams (user_id, team_id)
SELECT u.id, t.id
FROM users u
JOIN companies c ON c.domain = 'demo.hive.local'
JOIN teams t ON t.company_id = c.id AND t.name = 'Engineering'
WHERE u.email = 'dual@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_teams ut WHERE ut.user_id = u.id AND ut.team_id = t.id
  );

INSERT INTO user_teams (user_id, team_id)
SELECT u.id, t.id
FROM users u
JOIN companies c ON c.domain = 'antonian.hive.local'
JOIN teams t ON t.company_id = c.id AND t.name = 'Care Ops'
WHERE u.email = 'dual@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_teams ut WHERE ut.user_id = u.id AND ut.team_id = t.id
  );

-- Demo manager on Engineering; also on Clinical in Antonian
INSERT INTO user_teams (user_id, team_id)
SELECT u.id, t.id
FROM users u
JOIN companies c ON c.domain = 'demo.hive.local'
JOIN teams t ON t.company_id = c.id AND t.name = 'Engineering'
WHERE u.email = 'manager@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_teams ut WHERE ut.user_id = u.id AND ut.team_id = t.id
  );

INSERT INTO user_teams (user_id, team_id)
SELECT u.id, t.id
FROM users u
JOIN companies c ON c.domain = 'antonian.hive.local'
JOIN teams t ON t.company_id = c.id AND t.name = 'Clinical'
WHERE u.email = 'manager@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_teams ut WHERE ut.user_id = u.id AND ut.team_id = t.id
  );

-- Job titles for both companies (optional convenience)
INSERT INTO company_job_titles (company_id, title, active)
SELECT c.id, 'Engineering Lead', true
FROM companies c
WHERE c.domain = 'demo.hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM company_job_titles j WHERE j.company_id = c.id AND j.title = 'Engineering Lead'
  );

INSERT INTO company_job_titles (company_id, title, active)
SELECT c.id, 'Care Coordinator', true
FROM companies c
WHERE c.domain = 'antonian.hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM company_job_titles j WHERE j.company_id = c.id AND j.title = 'Care Coordinator'
  );
