package com.krishna.briefcase.cases.event;

import com.krishna.briefcase.cases.CaseStatus;

public record CaseStatusChangedEvent(Long caseId, CaseStatus oldStatus, CaseStatus newStatus, String changedBy) {
}
