package com.krishna.briefcase.outcome.dto;

import com.krishna.briefcase.outcome.CaseResult;
import java.math.BigDecimal;
import java.time.Instant;

public record CaseOutcomeResponse(
        Long id,
        Long caseId,
        CaseResult result,
        BigDecimal settlementAmount,
        BigDecimal grossRevenue,
        BigDecimal firmCut,
        BigDecimal netDistributable,
        String recordedBy,
        Instant recordedAt
) {
}
