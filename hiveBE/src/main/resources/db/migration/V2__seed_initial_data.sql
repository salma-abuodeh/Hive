INSERT INTO roles (name, description, company_id) VALUES
                                                      ('Employee', 'Standard user with access to community features', NULL),
                                                      ('Manager', 'Manages teams and company content', NULL),
                                                      ('Company Admin', 'Manages one company and its users', NULL);

INSERT INTO companies (name, type, domain, created_at) VALUES
    ('Hive Test Co', 'COMPANY', 'hivetest.local', now());

INSERT INTO users (full_name, email, password, created_at, company_id, role_id)
VALUES (
           'Test Admin',
           'admin@hivetest.local',
           '$2a$10$O30c8ayXmehva1PQntd1cO1HI0bOwxMBpAYb.YCX4v4S/UAYhtYNS',
           now(),
           (SELECT id FROM companies WHERE domain = 'hivetest.local'),
       (SELECT id FROM roles WHERE name = 'Company Admin' AND company_id IS NULL)
    );