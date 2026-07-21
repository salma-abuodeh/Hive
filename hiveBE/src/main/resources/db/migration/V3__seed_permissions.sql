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
    ('PLATFORM_MANAGE', 'Platform-wide administration', true);

-- Platform Administrator: all permissions
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'Platform Administrator'
  AND r.company_id IS NULL;

-- Manager: company + user management
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
    'TEAM_MANAGE_MEMBERS'
)
WHERE r.name = 'Manager'
  AND r.company_id IS NULL;
