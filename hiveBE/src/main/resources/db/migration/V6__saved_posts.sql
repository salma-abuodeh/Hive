CREATE TABLE saved_posts (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT    NOT NULL REFERENCES users(id),
    post_id     BIGINT    NOT NULL REFERENCES posts(id),
    created_at  TIMESTAMP NOT NULL DEFAULT now(),
    UNIQUE (user_id, post_id)
);

CREATE INDEX idx_saved_posts_user ON saved_posts(user_id);
CREATE INDEX idx_saved_posts_post ON saved_posts(post_id);
