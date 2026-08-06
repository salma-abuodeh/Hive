CREATE TABLE company_applications (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL REFERENCES users(id),
    name VARCHAR(255) NOT NULL,
    company_type VARCHAR(50) NOT NULL,
    domain VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT REFERENCES users(id),
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMP
);
CREATE INDEX idx_company_applications_status ON company_applications(status);

CREATE TABLE membership_requests (
    id BIGSERIAL PRIMARY KEY,
    requester_id BIGINT NOT NULL REFERENCES users(id),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewer_id BIGINT REFERENCES users(id),
    rejection_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    reviewed_at TIMESTAMP
);
CREATE INDEX idx_membership_requests_company_status ON membership_requests(company_id, status);

-- Additional accounts for the approval walkthrough (password: password123).
INSERT INTO users (platform_role_id, first_name, last_name, email, password_hash, job_title, status, active)
SELECT r.id, 'Scenario', 'Admin', 'scenario.admin@hive.local',
       '$2a$10$0RCcyNURbY2NvT6KLxMbD.3tFLsMkAEoztnHuu3nqbLb58q9SEcIK',
       'Platform Administrator', 'active', true
FROM roles r WHERE r.name = 'Platform Administrator' AND r.company_id IS NULL
  AND NOT EXISTS (SELECT 1 FROM users WHERE email = 'scenario.admin@hive.local');

INSERT INTO users (first_name, last_name, email, password_hash, job_title, status, active)
SELECT 'Scenario', 'Manager', 'scenario.manager@hive.local',
       '$2a$10$0RCcyNURbY2NvT6KLxMbD.3tFLsMkAEoztnHuu3nqbLb58q9SEcIK',
       'Company Applicant', 'active', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'scenario.manager@hive.local');

INSERT INTO users (first_name, last_name, email, password_hash, job_title, status, active)
SELECT 'Scenario', 'Employee', 'scenario.employee@hive.local',
       '$2a$10$0RCcyNURbY2NvT6KLxMbD.3tFLsMkAEoztnHuu3nqbLb58q9SEcIK',
       'Employee Applicant', 'active', true
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'scenario.employee@hive.local');
