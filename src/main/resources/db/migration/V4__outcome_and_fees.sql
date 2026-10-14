-- ============================================================
-- V4: Case financial resolution (Phase 6)
-- ============================================================

CREATE TABLE case_outcome (
    id                 BIGSERIAL PRIMARY KEY,
    case_id            BIGINT       NOT NULL UNIQUE REFERENCES legal_case(id),
    result             VARCHAR(30)  NOT NULL,  -- WON | LOST | SETTLED
    settlement_amount  NUMERIC(14,2),
    gross_revenue      NUMERIC(14,2) NOT NULL,
    firm_cut           NUMERIC(14,2) NOT NULL,
    net_distributable  NUMERIC(14,2) NOT NULL,
    recorded_by        VARCHAR(100) NOT NULL,
    recorded_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE fee_distribution (
    id           BIGSERIAL PRIMARY KEY,
    case_id      BIGINT       NOT NULL REFERENCES legal_case(id),
    attorney_id  BIGINT       NOT NULL REFERENCES attorney(id),
    share_type   VARCHAR(30)  NOT NULL,  -- LEAD_CUT | EQUAL_SPLIT | SOLE_LEAD
    amount       NUMERIC(14,2) NOT NULL,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    -- Backstops the financially_resolved flag: even a duplicate resolve() call can't
    -- double-insert a distribution for the same attorney on the same case.
    CONSTRAINT uq_fee_distribution_case_attorney UNIQUE (case_id, attorney_id)
);
CREATE INDEX idx_fee_distribution_case     ON fee_distribution (case_id);
CREATE INDEX idx_fee_distribution_attorney ON fee_distribution (attorney_id);
