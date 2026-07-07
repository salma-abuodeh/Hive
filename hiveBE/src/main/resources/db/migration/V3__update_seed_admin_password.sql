-- Update seed admin password to BCrypt hash of 'admin123'
UPDATE users
SET password = '$2a$10$O30c8ayXmehva1PQntd1cO1HI0bOwxMBpAYb.YCX4v4S/UAYhtYNS'
WHERE email = 'admin@hivetest.local';
