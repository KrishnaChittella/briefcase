package com.krishna.briefcase.cases.event;

import com.krishna.briefcase.cases.AssignmentRole;

public record CaseAssignmentChangedEvent(Long caseId, Long attorneyId, AssignmentRole role,
                                         ChangeType changeType, String changedBy) {
    public enum ChangeType {
        ASSIGNED,
        UNASSIGNED
    }
}
