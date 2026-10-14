package com.krishna.briefcase.cases.dto;

import com.krishna.briefcase.cases.BillingType;
import com.krishna.briefcase.cases.CaseStatus;
import java.math.BigDecimal;
import java.time.Instant;

public record CaseResponse(
        Long id,
        String caseNumber,
        String title,
        String description,
        Long clientId,
        String clientName,
        BillingType billingType,
        CaseStatus status,
        BigDecimal contingencyRate,
        BigDecimal hourlyRate,
        BigDecimal loggedHours,
        BigDecimal flatFee,
        boolean financiallyResolved,
        Instant openedAt,
        Instant resolvedAt,
        Instant closedAt,
        Long version
) {
}
