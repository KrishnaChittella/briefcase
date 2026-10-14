package com.krishna.briefcase.cases.dto;

import com.krishna.briefcase.cases.BillingType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

/** Composite request for creating a case and its initial attorney team in one call (Builder pattern). */
public record OnboardCaseRequest(
        @NotBlank String caseNumber,
        @NotBlank String title,
        String description,
        @NotNull Long clientId,
        @NotNull BillingType billingType,
        BigDecimal contingencyRate,
        BigDecimal hourlyRate,
        BigDecimal loggedHours,
        BigDecimal flatFee,
        @NotNull Long leadAttorneyId,
        List<Long> associateAttorneyIds,
        List<Long> paralegalAttorneyIds
) {
}
