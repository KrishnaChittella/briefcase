package com.krishna.briefcase.billing.dto;

import com.krishna.briefcase.billing.FeeShareType;
import java.math.BigDecimal;
import java.time.Instant;

public record FeeDistributionResponse(
        Long id,
        Long caseId,
        Long attorneyId,
        String attorneyName,
        FeeShareType shareType,
        BigDecimal amount,
        Instant createdAt
) {
}
