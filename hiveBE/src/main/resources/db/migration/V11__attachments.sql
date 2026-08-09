-- Generic attachment storage, reused later for event covers, post/comment images.
CREATE TABLE attachments (
                             id BIGSERIAL PRIMARY KEY,
                             company_id BIGINT NOT NULL REFERENCES companies(id),
                             uploaded_by_user_id BIGINT NOT NULL REFERENCES users(id),
                             context VARCHAR(30) NOT NULL,
                             storage_key VARCHAR(500) NOT NULL,
                             original_filename VARCHAR(255) NOT NULL,
                             content_type VARCHAR(100) NOT NULL,
                             size_bytes BIGINT NOT NULL,
                             created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX idx_attachments_company ON attachments(company_id);

-- users.profile_image_url was never wired up anywhere (checked BE + FE) — replacing
-- it with a real FK to attachments now that avatars are actually implemented.
ALTER TABLE users DROP COLUMN IF EXISTS profile_image_url;
ALTER TABLE users ADD COLUMN avatar_attachment_id BIGINT REFERENCES attachments(id);