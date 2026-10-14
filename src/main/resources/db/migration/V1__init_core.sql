-- ============================================================
-- V1: Core tables — client, attorney, legal_case, case_assignment
-- Attorney<->User link (user_id) is added in V2 (Phase 3).
-- ============================================================

CREATE TABLE client (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    email       VARCHAR(255),
    phone       VARCHAR(50),
    address     VARCHAR(500),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    version     BIGINT      NOT NULL DEFAULT 0
);

CREATE TABLE attorney (
    id          BIGSERIAL PRIMARY KEY,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    email       VARCHAR(255) NOT NULL UNIQUE,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version     BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE legal_case (
    id                    BIGSERIAL PRIMARY KEY,
    case_number           VARCHAR(50)  NOT NULL UNIQUE,
    title                 VARCHAR(255) NOT NULL,
    description           TEXT,
    client_id             BIGINT       NOT NULL REFERENCES client(id),
    billing_type          VARCHAR(30)  NOT NULL,          -- CONTINGENCY | HOURLY
    status                VARCHAR(30)  NOT NULL DEFAULT 'OPEN', -- OPEN|IN_PROGRESS|RESOLVED|CLOSED
    contingency_rate      NUMERIC(5,4),                   -- e.g. 0.3300 for contingency cases
    hourly_rate           NUMERIC(12,2),                  -- for hourly cases
    logged_hours          NUMERIC(10,2),                  -- for hourly cases
    financially_resolved  BOOLEAN      NOT NULL DEFAULT FALSE, -- idempotency guard (Phase 6)
    opened_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    resolved_at           TIMESTAMPTZ,
    closed_at             TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT now(),
    version               BIGINT       NOT NULL DEFAULT 0
);
CREATE INDEX idx_legal_case_client ON legal_case (client_id);
CREATE INDEX idx_legal_case_status ON legal_case (status);

CREATE TABLE case_assignment (
    id               BIGSERIAL PRIMARY KEY,
    case_id          BIGINT      NOT NULL REFERENCES legal_case(id) ON DELETE CASCADE,
    attorney_id      BIGINT      NOT NULL REFERENCES attorney(id),
    assignment_role  VARCHAR(30) NOT NULL,   -- LEAD | ASSOCIATE | PARALEGAL
    assigned_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_case_attorney UNIQUE (case_id, attorney_id)
);
CREATE INDEX idx_case_assignment_case     ON case_assignment (case_id);
CREATE INDEX idx_case_assignment_attorney ON case_assignment (attorney_id);
