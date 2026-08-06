-- Ensure existing scenario accounts use password123 even when V9 was previously applied.
UPDATE users
SET password_hash = '$2a$10$ks7MSukNj8gUyJNFLVWYHew3oELFp3Dyz1CUyPnNVKeqo3jeznjpe',
    updated_at = now()
WHERE email IN (
    'scenario.admin@hive.local',
    'scenario.manager@hive.local',
    'scenario.employee@hive.local'
);
