-- ==================== CORE TENANCY ====================

CREATE TABLE companies (
                           company_id      BIGSERIAL PRIMARY KEY,
                           name            VARCHAR(255) NOT NULL,
                           company_type    VARCHAR(50)  NOT NULL,
                           domain          VARCHAR(255) UNIQUE,
                           logo_url        VARCHAR(500),
                           status          VARCHAR(50)  NOT NULL DEFAULT 'pending',
                           active          BOOLEAN      NOT NULL DEFAULT true,
                           created_at      TIMESTAMP    NOT NULL DEFAULT now(),
                           updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE teams (
                       team_id         BIGSERIAL PRIMARY KEY,
                       company_id      BIGINT       NOT NULL REFERENCES companies(company_id),
                       name            VARCHAR(255) NOT NULL,
                       description     VARCHAR(500),
                       active          BOOLEAN      NOT NULL DEFAULT true,
                       created_at      TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE roles (
                       role_id         BIGSERIAL PRIMARY KEY,
                       company_id      BIGINT REFERENCES companies(company_id),
                       name            VARCHAR(100) NOT NULL,
                       description     VARCHAR(500),
                       active          BOOLEAN      NOT NULL DEFAULT true,
                       created_at      TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE users (
                       user_id           BIGSERIAL PRIMARY KEY,
                       company_id        BIGINT REFERENCES companies(company_id),
                       team_id           BIGINT REFERENCES teams(team_id),
                       role_id           BIGINT REFERENCES roles(role_id),
                       first_name        VARCHAR(100) NOT NULL,
                       last_name         VARCHAR(100) NOT NULL,
                       email             VARCHAR(255) NOT NULL UNIQUE,
                       password_hash     VARCHAR(255) NOT NULL,
                       job_title         VARCHAR(150),
                       profile_image_url VARCHAR(500),
                       status            VARCHAR(50)  NOT NULL DEFAULT 'active',
                       active            BOOLEAN      NOT NULL DEFAULT true,
                       created_at        TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at        TIMESTAMP    NOT NULL DEFAULT now()
);

-- ==================== FEED ====================

CREATE TABLE posts (
                       post_id         BIGSERIAL PRIMARY KEY,
                       company_id      BIGINT       NOT NULL REFERENCES companies(company_id),
                       author_user_id  BIGINT       NOT NULL REFERENCES users(user_id),
                       team_id         BIGINT REFERENCES teams(team_id),
                       post_type       VARCHAR(50)  NOT NULL,
                       content         TEXT         NOT NULL,
                       visibility_type VARCHAR(50)  NOT NULL DEFAULT 'company',
                       active          BOOLEAN      NOT NULL DEFAULT true,
                       created_at      TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE comments (
                          comment_id      BIGSERIAL PRIMARY KEY,
                          post_id         BIGINT    NOT NULL REFERENCES posts(post_id),
                          user_id         BIGINT    NOT NULL REFERENCES users(user_id),
                          content         TEXT      NOT NULL,
                          active          BOOLEAN   NOT NULL DEFAULT true,
                          created_at      TIMESTAMP NOT NULL DEFAULT now(),
                          updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reactions (
                           reaction_id     BIGSERIAL PRIMARY KEY,
                           user_id         BIGINT      NOT NULL REFERENCES users(user_id),
                           post_id         BIGINT REFERENCES posts(post_id),
                           comment_id      BIGINT REFERENCES comments(comment_id),
                           reaction_type   VARCHAR(50) NOT NULL,
                           created_at      TIMESTAMP   NOT NULL DEFAULT now(),
                           CONSTRAINT chk_reaction_target CHECK (
                               (post_id IS NOT NULL AND comment_id IS NULL) OR
                               (post_id IS NULL AND comment_id IS NOT NULL)
                               )
);
CREATE UNIQUE INDEX uq_reaction_post ON reactions(user_id, post_id) WHERE post_id IS NOT NULL;
CREATE UNIQUE INDEX uq_reaction_comment ON reactions(user_id, comment_id) WHERE comment_id IS NOT NULL;

CREATE TABLE follows (
                         follow_id         BIGSERIAL PRIMARY KEY,
                         follower_user_id  BIGINT    NOT NULL REFERENCES users(user_id),
                         followed_team_id  BIGINT REFERENCES teams(team_id),
                         followed_user_id  BIGINT REFERENCES users(user_id),
                         created_at        TIMESTAMP NOT NULL DEFAULT now(),
                         CONSTRAINT chk_follow_target CHECK (
                             (followed_team_id IS NOT NULL AND followed_user_id IS NULL) OR
                             (followed_team_id IS NULL AND followed_user_id IS NOT NULL)
                             ),
                         CONSTRAINT chk_no_self_follow CHECK (follower_user_id <> followed_user_id)
);
CREATE UNIQUE INDEX uq_follow_team ON follows(follower_user_id, followed_team_id) WHERE followed_team_id IS NOT NULL;
CREATE UNIQUE INDEX uq_follow_user ON follows(follower_user_id, followed_user_id) WHERE followed_user_id IS NOT NULL;

-- ==================== NOTIFICATIONS & LOGS ====================

CREATE TABLE notifications (
                               notification_id     BIGSERIAL PRIMARY KEY,
                               company_id           BIGINT      NOT NULL REFERENCES companies(company_id),
                               user_id               BIGINT      NOT NULL REFERENCES users(user_id),
                               notification_type    VARCHAR(50) NOT NULL,
                               message               TEXT        NOT NULL,
                               entity_type           VARCHAR(50),
                               entity_id              BIGINT,
                               is_read                BOOLEAN     NOT NULL DEFAULT false,
                               created_at             TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE logs (
                      log_id          BIGSERIAL PRIMARY KEY,
                      company_id      BIGINT      NOT NULL REFERENCES companies(company_id),
                      user_id         BIGINT      NOT NULL REFERENCES users(user_id),
                      action_type     VARCHAR(100) NOT NULL,
                      module_name     VARCHAR(100) NOT NULL,
                      entity_type     VARCHAR(50),
                      entity_id       BIGINT,
                      created_at      TIMESTAMP   NOT NULL DEFAULT now()
);

-- ==================== CHAT ====================

CREATE TABLE conversations (
                               conversation_id     BIGSERIAL PRIMARY KEY,
                               company_id          BIGINT      NOT NULL REFERENCES companies(company_id),
                               conversation_type   VARCHAR(50) NOT NULL,
                               name                VARCHAR(255),
                               created_at          TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE conversation_members (
                                      conversation_member_id BIGSERIAL PRIMARY KEY,
                                      conversation_id        BIGINT    NOT NULL REFERENCES conversations(conversation_id),
                                      user_id                 BIGINT    NOT NULL REFERENCES users(user_id),
                                      joined_at               TIMESTAMP NOT NULL DEFAULT now(),
                                      UNIQUE (conversation_id, user_id)
);

CREATE TABLE messages (
                          message_id       BIGSERIAL PRIMARY KEY,
                          conversation_id  BIGINT      NOT NULL REFERENCES conversations(conversation_id),
                          sender_user_id   BIGINT      NOT NULL REFERENCES users(user_id),
                          content          TEXT,
                          message_type     VARCHAR(50) NOT NULL DEFAULT 'text',
                          created_at       TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE message_attachments (
                                     attachment_id   BIGSERIAL PRIMARY KEY,
                                     message_id      BIGINT       NOT NULL REFERENCES messages(message_id),
                                     file_url        VARCHAR(500) NOT NULL,
                                     file_name       VARCHAR(255) NOT NULL,
                                     file_size       BIGINT,
                                     created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- ==================== EVENTS ====================

CREATE TABLE events (
                        event_id            BIGSERIAL PRIMARY KEY,
                        company_id          BIGINT       NOT NULL REFERENCES companies(company_id),
                        team_id             BIGINT REFERENCES teams(team_id),
                        created_by_user_id  BIGINT       NOT NULL REFERENCES users(user_id),
                        title               VARCHAR(255) NOT NULL,
                        description         TEXT,
                        location            VARCHAR(255),
                        start_time          TIMESTAMP    NOT NULL,
                        end_time            TIMESTAMP    NOT NULL,
                        visibility_type     VARCHAR(50)  NOT NULL DEFAULT 'company',
                        active              BOOLEAN      NOT NULL DEFAULT true,
                        created_at          TIMESTAMP    NOT NULL DEFAULT now(),
                        updated_at          TIMESTAMP    NOT NULL DEFAULT now(),
                        CONSTRAINT chk_event_time CHECK (end_time > start_time)
);

CREATE TABLE event_rsvps (
                             rsvp_id         BIGSERIAL PRIMARY KEY,
                             event_id        BIGINT      NOT NULL REFERENCES events(event_id),
                             user_id         BIGINT      NOT NULL REFERENCES users(user_id),
                             status          VARCHAR(50) NOT NULL DEFAULT 'going',
                             responded_at    TIMESTAMP   NOT NULL DEFAULT now(),
                             UNIQUE (event_id, user_id)
);

-- ==================== TASKS ====================

CREATE TABLE tasks (
                       task_id             BIGSERIAL PRIMARY KEY,
                       company_id          BIGINT       NOT NULL REFERENCES companies(company_id),
                       team_id             BIGINT REFERENCES teams(team_id),
                       created_by_user_id  BIGINT       NOT NULL REFERENCES users(user_id),
                       assigned_to_user_id BIGINT REFERENCES users(user_id),
                       title               VARCHAR(255) NOT NULL,
                       description         TEXT,
                       status              VARCHAR(50)  NOT NULL DEFAULT 'todo',
                       priority            VARCHAR(50),
                       due_date            DATE,
                       active              BOOLEAN      NOT NULL DEFAULT true,
                       created_at          TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at          TIMESTAMP    NOT NULL DEFAULT now()
);

-- ==================== INVENTORY ====================

CREATE TABLE inventory_items (
                                 item_id             BIGSERIAL PRIMARY KEY,
                                 company_id          BIGINT       NOT NULL REFERENCES companies(company_id),
                                 name                VARCHAR(255) NOT NULL,
                                 sku                 VARCHAR(100),
                                 quantity            INT          NOT NULL DEFAULT 0,
                                 unit                VARCHAR(50),
                                 low_stock_threshold INT,
                                 location            VARCHAR(255),
                                 active              BOOLEAN      NOT NULL DEFAULT true,
                                 created_at          TIMESTAMP    NOT NULL DEFAULT now(),
                                 updated_at          TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE inventory_logs (
                                inventory_log_id BIGSERIAL PRIMARY KEY,
                                item_id          BIGINT      NOT NULL REFERENCES inventory_items(item_id),
                                user_id          BIGINT      NOT NULL REFERENCES users(user_id),
                                change_type      VARCHAR(50) NOT NULL,
                                quantity_change  INT         NOT NULL,
                                note             VARCHAR(500),
                                created_at       TIMESTAMP   NOT NULL DEFAULT now()
);

-- ==================== TENANT-SCOPING INDEXES ====================

CREATE INDEX idx_teams_company ON teams(company_id);
CREATE INDEX idx_roles_company ON roles(company_id);
CREATE INDEX idx_users_company ON users(company_id);
CREATE INDEX idx_users_team ON users(team_id);
CREATE INDEX idx_users_role ON users(role_id);
CREATE INDEX idx_posts_company ON posts(company_id);
CREATE INDEX idx_posts_author ON posts(author_user_id);
CREATE INDEX idx_comments_post ON comments(post_id);
CREATE INDEX idx_notifications_company_user ON notifications(company_id, user_id);
CREATE INDEX idx_logs_company ON logs(company_id);
CREATE INDEX idx_conversations_company ON conversations(company_id);
CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_events_company ON events(company_id);
CREATE INDEX idx_tasks_company ON tasks(company_id);
CREATE INDEX idx_tasks_assigned ON tasks(assigned_to_user_id);
CREATE INDEX idx_inventory_items_company ON inventory_items(company_id);