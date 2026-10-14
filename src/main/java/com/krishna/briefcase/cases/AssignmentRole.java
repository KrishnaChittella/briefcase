package com.krishna.briefcase.cases;

/**
 * How an attorney/paralegal is attached to a specific case. Distinct from the security
 * {@code UserRole} — this only describes the case-level responsibility. Exactly one
 * assignment per case may be LEAD; PARALEGAL assignees are excluded from the attorney fee
 * split (Phase 6).
 */
public enum AssignmentRole {
    LEAD,
    ASSOCIATE,
    PARALEGAL
}
