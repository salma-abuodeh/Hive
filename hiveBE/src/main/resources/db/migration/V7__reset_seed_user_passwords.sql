-- password123
UPDATE users
SET password = '$2a$10$rV9/1/xSl9atUxPdSFpSBeIxS4ZlEGrMQ/W6QMPJ3W0IvHyBT9Rz2'
WHERE email IN (
    'platform@hive.local',
    'admin@hivetest.local'
);
