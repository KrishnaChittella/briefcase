package com.krishna.briefcase.outcome;

import java.math.BigDecimal;

public record CaseOutcomeRecordedEvent(Long caseId, CaseResult result, BigDecimal grossRevenue,
                                       BigDecimal firmCut, BigDecimal netDistributable, String recordedBy) {
}
