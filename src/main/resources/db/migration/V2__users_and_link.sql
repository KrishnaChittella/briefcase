-- ============================================================
-- V2: Login users + Attorney<->User link (Phase 3/4)
-- ============================================================

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    username      VARCHAR(100) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(30)  NOT NULL,  -- SUPER_ADMIN|MANAGING_PARTNER|SENIOR_ATTORNEY|JUNIOR_ATTORNEY|PARALEGAL|STAFF
    enabled       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version       BIGINT       NOT NULL DEFAULT 0
);

-- An attorney logs in as themselves; 1:1 optional link to a login user.
ALTER TABLE attorney
    ADD COLUMN user_id BIGINT UNIQUE REFERENCES users(id);
