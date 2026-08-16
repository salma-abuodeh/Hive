-- Local chat-testing fixture. Every account below uses password: password123
-- The statements are idempotent so existing development databases are safe.

INSERT INTO users (first_name, last_name, email, password_hash, job_title, status, active)
SELECT first_name, last_name, email,
       '$2a$10$ks7MSukNj8gUyJNFLVWYHew3oELFp3Dyz1CUyPnNVKeqo3jeznjpe',
       job_title, 'active', true
FROM (VALUES
    ('Chat', 'Manager', 'chat.manager@hive.local', 'Chat Test Manager'),
    ('Alex', 'Chen', 'chat.alex@hive.local', 'Chat Test Member'),
    ('Sam', 'Jordan', 'chat.sam@hive.local', 'Chat Test Member')
) AS seed(first_name, last_name, email, job_title)
WHERE NOT EXISTS (SELECT 1 FROM users u WHERE u.email = seed.email);

-- All three users are members of Demo Company. The manager can create and
-- manage teams; the other two exercise regular member chat behavior.
INSERT INTO user_companies (user_id, company_id, role_id, active)
SELECT u.id, c.id, r.id, true
FROM users u
JOIN companies c ON c.domain = 'demo.hive.local'
JOIN roles r ON r.name = 'Manager' AND r.company_id IS NULL
WHERE u.email = 'chat.manager@hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM user_companies uc WHERE uc.user_id = u.id AND uc.company_id = c.id
  );

INSERT INTO user_companies (user_id, company_id, role_id, active)
SELECT u.id, c.id, r.id, true
FROM users u
JOIN companies c ON c.domain = 'demo.hive.local'
JOIN roles r ON r.name = 'Employee' AND r.company_id IS NULL
WHERE u.email IN ('chat.alex@hive.local', 'chat.sam@hive.local')
  AND NOT EXISTS (
      SELECT 1 FROM user_companies uc WHERE uc.user_id = u.id AND uc.company_id = c.id
  );

INSERT INTO teams (company_id, name, description, active)
SELECT c.id, 'Chat Test Team', 'Team used to exercise team conversations', true
FROM companies c
WHERE c.domain = 'demo.hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM teams t WHERE t.company_id = c.id AND t.name = 'Chat Test Team'
  );

INSERT INTO user_teams (user_id, team_id)
SELECT u.id, t.id
FROM users u
JOIN companies c ON c.domain = 'demo.hive.local'
JOIN teams t ON t.company_id = c.id AND t.name = 'Chat Test Team'
WHERE u.email IN ('chat.manager@hive.local', 'chat.alex@hive.local', 'chat.sam@hive.local')
  AND NOT EXISTS (
      SELECT 1 FROM user_teams ut WHERE ut.user_id = u.id AND ut.team_id = t.id
  );

-- Teams created through TeamService automatically receive this conversation.
-- It is seeded explicitly here because Flyway migrations run at the SQL layer.
INSERT INTO conversations (company_id, conversation_type, team_id)
SELECT c.id, 'TEAM', t.id
FROM companies c
JOIN teams t ON t.company_id = c.id AND t.name = 'Chat Test Team'
WHERE c.domain = 'demo.hive.local'
  AND NOT EXISTS (SELECT 1 FROM conversations cv WHERE cv.team_id = t.id);

INSERT INTO conversation_members (conversation_id, user_id)
SELECT cv.id, u.id
FROM conversations cv
JOIN teams t ON t.id = cv.team_id AND t.name = 'Chat Test Team'
JOIN companies c ON c.id = cv.company_id AND c.domain = 'demo.hive.local'
JOIN users u ON u.email IN ('chat.manager@hive.local', 'chat.alex@hive.local', 'chat.sam@hive.local')
WHERE NOT EXISTS (
    SELECT 1 FROM conversation_members cm WHERE cm.conversation_id = cv.id AND cm.user_id = u.id
);

-- A group including all three members. This is the closest current equivalent
-- to a company chat; the application has no COMPANY conversation type yet.
INSERT INTO conversations (company_id, conversation_type, created_by_user_id, name)
SELECT c.id, 'GROUP', creator.id, 'Company Lounge'
FROM companies c
JOIN users creator ON creator.email = 'chat.manager@hive.local'
WHERE c.domain = 'demo.hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM conversations cv
      WHERE cv.company_id = c.id AND cv.conversation_type = 'GROUP' AND cv.name = 'Company Lounge'
  );

INSERT INTO conversation_members (conversation_id, user_id)
SELECT cv.id, u.id
FROM conversations cv
JOIN companies c ON c.id = cv.company_id AND c.domain = 'demo.hive.local'
JOIN users u ON u.email IN ('chat.manager@hive.local', 'chat.alex@hive.local', 'chat.sam@hive.local')
WHERE cv.conversation_type = 'GROUP' AND cv.name = 'Company Lounge'
  AND NOT EXISTS (
      SELECT 1 FROM conversation_members cm WHERE cm.conversation_id = cv.id AND cm.user_id = u.id
  );

-- A direct conversation between the manager and Alex.
INSERT INTO conversations (company_id, conversation_type, direct_key)
SELECT c.id, 'DIRECT', LEAST(manager.id, alex.id)::text || '_' || GREATEST(manager.id, alex.id)::text
FROM companies c
JOIN users manager ON manager.email = 'chat.manager@hive.local'
JOIN users alex ON alex.email = 'chat.alex@hive.local'
WHERE c.domain = 'demo.hive.local'
  AND NOT EXISTS (
      SELECT 1 FROM conversations cv
      WHERE cv.company_id = c.id
        AND cv.direct_key = LEAST(manager.id, alex.id)::text || '_' || GREATEST(manager.id, alex.id)::text
  );

INSERT INTO conversation_members (conversation_id, user_id)
SELECT cv.id, u.id
FROM conversations cv
JOIN companies c ON c.id = cv.company_id AND c.domain = 'demo.hive.local'
JOIN users manager ON manager.email = 'chat.manager@hive.local'
JOIN users alex ON alex.email = 'chat.alex@hive.local'
JOIN users u ON u.id IN (manager.id, alex.id)
WHERE cv.conversation_type = 'DIRECT'
  AND cv.direct_key = LEAST(manager.id, alex.id)::text || '_' || GREATEST(manager.id, alex.id)::text
  AND NOT EXISTS (
      SELECT 1 FROM conversation_members cm WHERE cm.conversation_id = cv.id AND cm.user_id = u.id
  );
