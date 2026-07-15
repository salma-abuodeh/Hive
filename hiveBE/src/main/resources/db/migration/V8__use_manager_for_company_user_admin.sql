-- Seed company admin account should use Manager (company user management role)
UPDATE users
SET role_id = (SELECT id FROM roles WHERE name = 'Manager' AND company_id IS NULL)
WHERE email = 'admin@hivetest.local'
  AND EXISTS (SELECT 1 FROM roles WHERE name = 'Manager' AND company_id IS NULL);
