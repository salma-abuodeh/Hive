-- Seed chat permission catalog (names match org.example.hive.security.Permissions)
INSERT INTO permissions (name, description, active) VALUES
                                                        ('CONVERSATION_VIEW', 'View conversations you are a member of', true),
                                                        ('CONVERSATION_CREATE', 'Start direct, group, or team conversations', true),
                                                        ('CONVERSATION_MANAGE_MEMBERS', 'Manage members of any conversation in the company (override)', true),
                                                        ('MESSAGE_CREATE', 'Send messages in a conversation', true),
                                                        ('MESSAGE_DELETE', 'Delete any message in the company (override)', true);

-- Platform Administrator already receives every permission via the existing
-- cross-join seed in V3, so nothing to add there.

-- Manager: baseline chat use + moderation overrides
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN (
                                          'CONVERSATION_VIEW',
                                          'CONVERSATION_CREATE',
                                          'CONVERSATION_MANAGE_MEMBERS',
                                          'MESSAGE_CREATE',
                                          'MESSAGE_DELETE'
    )
WHERE r.name = 'Manager'
  AND r.company_id IS NULL;

-- Employee: baseline chat use (own messages managed via ownership checks, not permissions)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN (
                                          'CONVERSATION_VIEW',
                                          'CONVERSATION_CREATE',
                                          'MESSAGE_CREATE'
    )
WHERE r.name = 'Employee'
  AND r.company_id IS NULL;