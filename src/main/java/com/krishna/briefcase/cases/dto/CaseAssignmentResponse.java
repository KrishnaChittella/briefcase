package com.krishna.briefcase.cases.dto;

import com.krishna.briefcase.cases.AssignmentRole;
import java.time.Instant;

public record CaseAssignmentResponse(
        Long id,
        Long caseId,
        Long attorneyId,
        String attorneyName,
        AssignmentRole assignmentRole,
        Instant assignedAt
) {
}
