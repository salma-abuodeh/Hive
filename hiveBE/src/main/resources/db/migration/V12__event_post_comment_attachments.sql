-- Event cover image (single, mirrors users.avatar_attachment_id).
ALTER TABLE events ADD COLUMN cover_attachment_id BIGINT REFERENCES attachments(id);

-- Post and comment attachments (0..N each). The attachment is the "many" side,
-- so it owns the FK back to whichever entity it belongs to. Exactly one of
-- these (plus the existing implicit "avatar"/"cover" ownership from the other
-- side) should be set per row, enforced in application code.
ALTER TABLE attachments ADD COLUMN post_id BIGINT REFERENCES posts(id);
ALTER TABLE attachments ADD COLUMN comment_id BIGINT REFERENCES comments(id);

CREATE INDEX idx_attachments_post ON attachments(post_id);
CREATE INDEX idx_attachments_comment ON attachments(comment_id);

-- First-class IMAGE/DOCUMENT distinction so features can filter or render
-- appropriately without sniffing content_type. Backfill existing rows (avatars
-- only so far) as IMAGE, then drop the default so future inserts must set it.
ALTER TABLE attachments ADD COLUMN attachment_type VARCHAR(20) NOT NULL DEFAULT 'IMAGE';
ALTER TABLE attachments ALTER COLUMN attachment_type DROP DEFAULT;