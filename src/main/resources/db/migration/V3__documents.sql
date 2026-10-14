-- ============================================================
-- V3: Case document uploads (Phase 5)
-- ============================================================

CREATE TABLE document (
    id            BIGSERIAL PRIMARY KEY,
    case_id       BIGINT       NOT NULL REFERENCES legal_case(id) ON DELETE CASCADE,
    file_name     VARCHAR(255) NOT NULL,
    storage_path  VARCHAR(500) NOT NULL,
    content_type  VARCHAR(150),
    size_bytes    BIGINT       NOT NULL,
    uploaded_by   VARCHAR(100) NOT NULL,
    uploaded_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_document_case ON document (case_id);
