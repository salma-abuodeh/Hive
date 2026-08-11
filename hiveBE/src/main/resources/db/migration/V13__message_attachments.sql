-- Message attachments (0..N per message). Same "many side owns the FK" shape
-- as post_id/comment_id in V12 — exactly one owner column should be set per
-- row, enforced in application code.
ALTER TABLE attachments ADD COLUMN message_id BIGINT REFERENCES messages(id);

CREATE INDEX idx_attachments_message ON attachments(message_id);