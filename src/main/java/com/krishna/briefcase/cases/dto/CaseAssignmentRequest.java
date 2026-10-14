package com.krishna.briefcase.cases.dto;

import com.krishna.briefcase.cases.AssignmentRole;
import jakarta.validation.constraints.NotNull;

public record CaseAssignmentRequest(
        @NotNull Long attorneyId,
        @NotNull AssignmentRole assignmentRole
) {
}
