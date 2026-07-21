-- Fix default user passwords so Spring BCryptPasswordEncoder accepts them (password: password123)
UPDATE users
SET password_hash = '$2a$10$0RCcyNURbY2NvT6KLxMbD.3tFLsMkAEoztnHuu3nqbLb58q9SEcIK',
    updated_at = now()
WHERE email IN ('admin@hive.local', 'manager@hive.local', 'employee@hive.local');
