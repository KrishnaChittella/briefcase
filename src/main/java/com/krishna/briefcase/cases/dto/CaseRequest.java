package com.krishna.briefcase.cases.dto;

import com.krishna.briefcase.cases.BillingType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Create/update payload for a case. On update, caseNumber and client are not changed.
 * Cross-field rules (contingency requires a rate, hourly requires rate+hours,
 * flat fee requires the fee) are
 * enforced in the service layer.
 */
public record CaseRequest(
        @NotBlank @Size(max = 50) String caseNumber,
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotNull Long clientId,
        @NotNull BillingType billingType,
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal contingencyRate,
        @DecimalMin("0.0") BigDecimal hourlyRate,
        @DecimalMin("0.0") BigDecimal loggedHours,
        @DecimalMin("0.0") BigDecimal flatFee
) {
}
