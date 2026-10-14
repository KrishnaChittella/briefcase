-- ============================================================
-- V5: Audit trail (Phase 7)
-- ============================================================

CREATE TABLE audit_log (
    id           BIGSERIAL PRIMARY KEY,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    BIGINT       NOT NULL,
    action       VARCHAR(50)  NOT NULL,
    performed_by VARCHAR(100) NOT NULL,
    occurred_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    details      TEXT
);
CREATE INDEX idx_audit_log_entity ON audit_log (entity_type, entity_id);
