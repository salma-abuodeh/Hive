-- ==================== CORE TENANCY ====================

CREATE TABLE companies (
                           id              BIGSERIAL PRIMARY KEY,
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
                       id              BIGSERIAL PRIMARY KEY,
                       company_id      BIGINT       NOT NULL REFERENCES companies(id),
                       name            VARCHAR(255) NOT NULL,
                       description     VARCHAR(500),
                       active          BOOLEAN      NOT NULL DEFAULT true,
                       created_at      TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE roles (
                       id              BIGSERIAL PRIMARY KEY,
                       company_id      BIGINT REFERENCES companies(id),
                       name            VARCHAR(100) NOT NULL,
                       description     VARCHAR(500),
                       active          BOOLEAN      NOT NULL DEFAULT true,
                       created_at      TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- ==================== PERMISSIONS ====================

CREATE TABLE permissions (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL UNIQUE,
    description     VARCHAR(500),
    active          BOOLEAN      NOT NULL DEFAULT true,
    created_at      TIMESTAMP    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE role_permissions (
                                  id              BIGSERIAL PRIMARY KEY,
                                  role_id         BIGINT NOT NULL REFERENCES roles(id),
                                  permission_id   BIGINT NOT NULL REFERENCES permissions(id),
                                  created_at      TIMESTAMP NOT NULL DEFAULT now(),
                                  UNIQUE (role_id, permission_id)
);

-- ==================== USERS ====================
-- A user is a platform-wide identity. Company/team membership and the role
-- held within each is modeled separately (user_companies / user_teams)
-- since a user can belong to more than one company or team.
-- platform_role_id is separate from company membership entirely: it's only
-- ever set for platform-level staff (e.g. Platform Admin), who may belong
-- to no company at all.

CREATE TABLE users (
                       id                BIGSERIAL PRIMARY KEY,
                       platform_role_id  BIGINT REFERENCES roles(id),
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

CREATE TABLE user_companies (
                                id              BIGSERIAL PRIMARY KEY,
                                user_id         BIGINT NOT NULL REFERENCES users(id),
                                company_id      BIGINT NOT NULL REFERENCES companies(id),
                                role_id         BIGINT NOT NULL REFERENCES roles(id),
                                active          BOOLEAN NOT NULL DEFAULT true,
                                joined_at       TIMESTAMP NOT NULL DEFAULT now(),
                                UNIQUE (user_id, company_id)
);

CREATE TABLE user_teams (
                            id              BIGSERIAL PRIMARY KEY,
                            user_id         BIGINT NOT NULL REFERENCES users(id),
                            team_id         BIGINT NOT NULL REFERENCES teams(id),
                            joined_at       TIMESTAMP NOT NULL DEFAULT now(),
                            UNIQUE (user_id, team_id)
);

-- ==================== FEED ====================

CREATE TABLE posts (
                       id              BIGSERIAL PRIMARY KEY,
                       company_id      BIGINT       NOT NULL REFERENCES companies(id),
                       author_user_id  BIGINT       NOT NULL REFERENCES users(id),
                       team_id         BIGINT REFERENCES teams(id),
                       post_type       VARCHAR(50)  NOT NULL,
                       content         TEXT         NOT NULL,
                       visibility_type VARCHAR(50)  NOT NULL DEFAULT 'company',
                       active          BOOLEAN      NOT NULL DEFAULT true,
                       created_at      TIMESTAMP    NOT NULL DEFAULT now(),
                       updated_at      TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE TABLE comments (
                          id              BIGSERIAL PRIMARY KEY,
                          post_id         BIGINT    NOT NULL REFERENCES posts(id),
                          user_id         BIGINT    NOT NULL REFERENCES users(id),
                          content         TEXT      NOT NULL,
                          active          BOOLEAN   NOT NULL DEFAULT true,
                          created_at      TIMESTAMP NOT NULL DEFAULT now(),
                          updated_at      TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE reactions (
                           id              BIGSERIAL PRIMARY KEY,
                           user_id         BIGINT      NOT NULL REFERENCES users(id),
                           post_id         BIGINT REFERENCES posts(id),
                           comment_id      BIGINT REFERENCES comments(id),
                           reaction_type   VARCHAR(50) NOT NULL,
                           created_at      TIMESTAMP   NOT NULL DEFAULT now(),
                           CONSTRAINT chk_reaction_target CHECK (
                               (post_id IS NOT NULL AND comment_id IS NULL) OR
                               (post_id IS NULL AND comment_id IS NOT NULL)
                               )
);
CREATE UNIQUE INDEX uq_reaction_post ON reactions(user_id, post_id) WHERE post_id IS NOT NULL;
CREATE UNIQUE INDEX uq_reaction_comment ON reactions(user_id, comment_id) WHERE comment_id IS NOT NULL;

-- ==================== NOTIFICATIONS & LOGS ====================

CREATE TABLE notifications (
                               id                     BIGSERIAL PRIMARY KEY,
                               company_id             BIGINT      NOT NULL REFERENCES companies(id),
                               user_id                BIGINT      NOT NULL REFERENCES users(id),
                               notification_type      VARCHAR(50) NOT NULL,
                               message                TEXT        NOT NULL,
                               entity_type            VARCHAR(50),
                               entity_id              BIGINT,
                               is_read                BOOLEAN     NOT NULL DEFAULT false,
                               created_at             TIMESTAMP   NOT NULL DEFAULT now()
);

-- old_value / new_value capture the before/after state of whatever changed,
-- so an audit entry is actually useful, not just "something happened".
CREATE TABLE logs (
                      id              BIGSERIAL PRIMARY KEY,
                      company_id      BIGINT       NOT NULL REFERENCES companies(id),
                      user_id         BIGINT       NOT NULL REFERENCES users(id),
                      action_type     VARCHAR(100) NOT NULL,
                      module_name     VARCHAR(100) NOT NULL,
                      entity_type     VARCHAR(50),
                      entity_id       BIGINT,
                      old_value       TEXT,
                      new_value       TEXT,
                      created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- ==================== CHAT ====================

CREATE TABLE conversations (
                               id                  BIGSERIAL PRIMARY KEY,
                               company_id          BIGINT      NOT NULL REFERENCES companies(id),
                               conversation_type   VARCHAR(50) NOT NULL,
                               name                VARCHAR(255),
                               created_at          TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE conversation_members (
                                      id                      BIGSERIAL PRIMARY KEY,
                                      conversation_id         BIGINT    NOT NULL REFERENCES conversations(id),
                                      user_id                 BIGINT    NOT NULL REFERENCES users(id),
                                      joined_at               TIMESTAMP NOT NULL DEFAULT now(),
                                      UNIQUE (conversation_id, user_id)
);

CREATE TABLE messages (
                          id               BIGSERIAL PRIMARY KEY,
                          conversation_id  BIGINT      NOT NULL REFERENCES conversations(id),
                          sender_user_id   BIGINT      NOT NULL REFERENCES users(id),
                          content          TEXT,
                          message_type     VARCHAR(50) NOT NULL DEFAULT 'text',
                          created_at       TIMESTAMP   NOT NULL DEFAULT now()
);

CREATE TABLE message_attachments (
                                     id              BIGSERIAL PRIMARY KEY,
                                     message_id      BIGINT       NOT NULL REFERENCES messages(id),
                                     file_url        VARCHAR(500) NOT NULL,
                                     file_name       VARCHAR(255) NOT NULL,
                                     file_size       BIGINT,
                                     created_at      TIMESTAMP    NOT NULL DEFAULT now()
);

-- ==================== EVENTS ====================

CREATE TABLE events (
                        id                  BIGSERIAL PRIMARY KEY,
                        company_id          BIGINT       NOT NULL REFERENCES companies(id),
                        team_id             BIGINT REFERENCES teams(id),
                        created_by_user_id  BIGINT       NOT NULL REFERENCES users(id),
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
                             id              BIGSERIAL PRIMARY KEY,
                             event_id        BIGINT      NOT NULL REFERENCES events(id),
                             user_id         BIGINT      NOT NULL REFERENCES users(id),
                             status          VARCHAR(50) NOT NULL DEFAULT 'going',
                             responded_at    TIMESTAMP   NOT NULL DEFAULT now(),
                             UNIQUE (event_id, user_id)
);

-- ==================== TENANT-SCOPING INDEXES ====================

CREATE INDEX idx_teams_company ON teams(company_id);
CREATE INDEX idx_roles_company ON roles(company_id);
CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);
CREATE INDEX idx_users_platform_role ON users(platform_role_id);
CREATE INDEX idx_user_companies_user ON user_companies(user_id);
CREATE INDEX idx_user_companies_company ON user_companies(company_id);
CREATE INDEX idx_user_teams_user ON user_teams(user_id);
CREATE INDEX idx_user_teams_team ON user_teams(team_id);
CREATE INDEX idx_posts_company ON posts(company_id);
CREATE INDEX idx_posts_author ON posts(author_user_id);
CREATE INDEX idx_comments_post ON comments(post_id);
CREATE INDEX idx_notifications_company_user ON notifications(company_id, user_id);
CREATE INDEX idx_logs_company ON logs(company_id);
CREATE INDEX idx_conversations_company ON conversations(company_id);
CREATE INDEX idx_messages_conversation ON messages(conversation_id);
CREATE INDEX idx_events_company ON events(company_id);