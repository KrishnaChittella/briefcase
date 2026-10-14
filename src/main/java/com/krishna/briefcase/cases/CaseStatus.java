package com.krishna.briefcase.cases;

import java.util.Set;

/**
 * Case lifecycle. Result (WON/LOST/SETTLED) is tracked separately on CaseOutcome.
 * Allowed transitions are enforced in Phase 5.
 */
public enum CaseStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    private Set<CaseStatus> allowedNext() {
        return switch (this) {
            case OPEN -> Set.of(IN_PROGRESS);
            case IN_PROGRESS -> Set.of(RESOLVED);
            case RESOLVED -> Set.of(CLOSED);
            case CLOSED -> Set.of();
        };
    }

    public boolean canTransitionTo(CaseStatus target) {
        return allowedNext().contains(target);
    }
}
