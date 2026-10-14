package com.krishna.briefcase.outcome;

import com.krishna.briefcase.outcome.dto.CaseOutcomeResponse;
import org.springframework.stereotype.Component;

@Component
public class CaseOutcomeMapper {

    public CaseOutcomeResponse toResponse(CaseOutcome outcome) {
        return new CaseOutcomeResponse(
                outcome.getId(),
                outcome.getLegalCase().getId(),
                outcome.getResult(),
                outcome.getSettlementAmount(),
                outcome.getGrossRevenue(),
                outcome.getFirmCut(),
                outcome.getNetDistributable(),
                outcome.getRecordedBy(),
                outcome.getRecordedAt()
        );
    }
}
