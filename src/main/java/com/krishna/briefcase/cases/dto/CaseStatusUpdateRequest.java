package com.krishna.briefcase.cases.dto;

import com.krishna.briefcase.cases.CaseStatus;
import jakarta.validation.constraints.NotNull;

public record CaseStatusUpdateRequest(@NotNull CaseStatus status) {
}
