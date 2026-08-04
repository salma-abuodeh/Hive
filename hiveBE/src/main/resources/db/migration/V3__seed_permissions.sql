-- Seed permission catalog (names match org.example.hive.security.Permissions)
INSERT INTO permissions (name, description, active) VALUES
                                                        ('COMPANY_VIEW', 'View company details', true),
                                                        ('COMPANY_UPDATE', 'Update company details', true),
                                                        ('COMPANY_UPDATE_BRANDING', 'Update company branding', true),
                                                        ('COMPANY_ARCHIVE', 'Archive a company', true),
                                                        ('TEAM_VIEW', 'View teams', true),
                                                        ('TEAM_CREATE', 'Create teams', true),
                                                        ('TEAM_UPDATE', 'Update teams', true),
                                                        ('TEAM_DELETE', 'Delete teams', true),
                                                        ('TEAM_MANAGE_MEMBERS', 'Manage team members', true),
                                                        ('ROLE_VIEW', 'View roles', true),
                                                        ('ROLE_CREATE', 'Create roles', true),
                                                        ('ROLE_UPDATE', 'Update roles', true),
                                                        ('ROLE_DELETE', 'Delete roles', true),
                                                        ('ROLE_MANAGE_PERMISSIONS', 'Manage role permissions', true),
                                                        ('USER_VIEW', 'View users in a company', true),
                                                        ('USER_INVITE', 'Invite users to a company', true),
                                                        ('USER_UPDATE', 'Update users in a company', true),
                                                        ('USER_DEACTIVATE', 'Deactivate users in a company', true),
                                                        ('USER_ASSIGN_ROLE', 'Assign roles to users', true),
                                                        ('USER_RESET_PASSWORD', 'Reset user passwords', true),
                                                        ('EVENT_VIEW', 'View events', true),
                                                        ('EVENT_CREATE', 'Create events', true),
                                                        ('EVENT_UPDATE', 'Update any event in the company (override)', true),
                                                        ('EVENT_DELETE', 'Delete any event in the company (override)', true),
                                                        ('EVENT_INVITE', 'Manage invitations for any event in the company (override)', true),
                                                        ('POLL_VIEW', 'View polls', true),
                                                        ('POLL_CREATE', 'Create polls', true),
                                                        ('POLL_UPDATE', 'Update any poll in the company (override)', true),
                                                        ('POLL_DELETE', 'Delete any poll in the company (override)', true),
                                                        ('PLATFORM_MANAGE', 'Platform-wide administration', true);

-- Platform Administrator: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         CROSS JOIN permissions p
WHERE r.name = 'Platform Administrator'
  AND r.company_id IS NULL;

-- Manager: company + user management + events + polls (including overrides)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN (
                                          'COMPANY_VIEW',
                                          'COMPANY_UPDATE',
                                          'COMPANY_UPDATE_BRANDING',
                                          'COMPANY_ARCHIVE',
                                          'USER_VIEW',
                                          'USER_INVITE',
                                          'USER_UPDATE',
                                          'USER_DEACTIVATE',
                                          'USER_ASSIGN_ROLE',
                                          'USER_RESET_PASSWORD',
                                          'TEAM_VIEW',
                                          'TEAM_CREATE',
                                          'TEAM_UPDATE',
                                          'TEAM_DELETE',
                                          'TEAM_MANAGE_MEMBERS',
                                          'EVENT_VIEW',
                                          'EVENT_CREATE',
                                          'EVENT_UPDATE',
                                          'EVENT_DELETE',
                                          'EVENT_INVITE',
                                          'POLL_VIEW',
                                          'POLL_CREATE',
                                          'POLL_UPDATE',
                                          'POLL_DELETE'
    )
WHERE r.name = 'Manager'
  AND r.company_id IS NULL;

-- Employee: baseline visibility + community features (view-only, plus own events/polls)
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
         JOIN permissions p ON p.name IN (
                                          'COMPANY_VIEW',
                                          'TEAM_VIEW',
                                          'USER_VIEW',
                                          'EVENT_VIEW',
                                          'EVENT_CREATE',
                                          'POLL_VIEW',
                                          'POLL_CREATE'
    )
WHERE r.name = 'Employee'
  AND r.company_id IS NULL;