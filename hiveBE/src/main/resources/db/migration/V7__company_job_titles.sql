CREATE TABLE company_job_titles (
    id          BIGSERIAL PRIMARY KEY,
    company_id  BIGINT       NOT NULL REFERENCES companies(id),
    title       VARCHAR(150) NOT NULL,
    active      BOOLEAN      NOT NULL DEFAULT true,
    created_at  TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMP    NOT NULL DEFAULT now(),
    UNIQUE (company_id, title)
);

CREATE INDEX idx_company_job_titles_company ON company_job_titles(company_id);

ALTER TABLE user_companies
    ADD COLUMN job_title_id BIGINT REFERENCES company_job_titles(id);

CREATE INDEX idx_user_companies_job_title ON user_companies(job_title_id);
