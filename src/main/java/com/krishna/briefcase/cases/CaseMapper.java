package com.krishna.briefcase.cases;

import com.krishna.briefcase.cases.dto.CaseResponse;
import org.springframework.stereotype.Component;

@Component
public class CaseMapper {

    public CaseResponse toResponse(LegalCase c) {
        return new CaseResponse(
                c.getId(),
                c.getCaseNumber(),
                c.getTitle(),
                c.getDescription(),
                c.getClient().getId(),
                c.getClient().getName(),
                c.getBillingType(),
                c.getStatus(),
                c.getContingencyRate(),
                c.getHourlyRate(),
                c.getLoggedHours(),
                c.getFlatFee(),
                c.isFinanciallyResolved(),
                c.getOpenedAt(),
                c.getResolvedAt(),
                c.getClosedAt(),
                c.getVersion()
        );
    }
}
