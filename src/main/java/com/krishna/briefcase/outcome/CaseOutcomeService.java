package com.krishna.briefcase.outcome;

import com.krishna.briefcase.billing.CaseFinancialResolver;
import com.krishna.briefcase.cases.CaseService;
import com.krishna.briefcase.cases.CaseStatus;
import com.krishna.briefcase.cases.LegalCase;
import com.krishna.briefcase.common.exception.InvalidCaseStateException;
import com.krishna.briefcase.common.exception.ResourceNotFoundException;
import com.krishna.briefcase.common.security.CurrentUser;
import com.krishna.briefcase.outcome.dto.CaseOutcomeResponse;
import com.krishna.briefcase.outcome.dto.RecordOutcomeRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseOutcomeService {

    private final CaseOutcomeRepository repository;
    private final CaseOutcomeMapper mapper;
    private final CaseService caseService;
    private final CaseFinancialResolver resolver;
    private final ApplicationEventPublisher eventPublisher;

    public CaseOutcomeService(CaseOutcomeRepository repository, CaseOutcomeMapper mapper,
                              CaseService caseService, CaseFinancialResolver resolver,
                              ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.caseService = caseService;
        this.resolver = resolver;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Records the case's legal result and, in the same transaction, runs the fee-distribution
     * engine. Guarded twice against double-running: the {@code financiallyResolved} flag here,
     * and a unique (case, attorney) constraint on fee_distribution as a DB-level backstop.
     */
    @Transactional
    public CaseOutcomeResponse recordOutcome(Long caseId, RecordOutcomeRequest request) {
        LegalCase legalCase = caseService.getEntity(caseId);

        if (legalCase.getStatus() != CaseStatus.RESOLVED) {
            throw new InvalidCaseStateException("Case " + caseId
                    + " must be RESOLVED before recording an outcome (current status: " + legalCase.getStatus() + ")");
        }
        if (legalCase.isFinanciallyResolved()) {
            throw new InvalidCaseStateException("Case " + caseId + " has already been financially resolved");
        }

        CaseFinancialResolver.Resolution resolution = resolver.resolve(legalCase, request.settlementAmount());

        CaseOutcome outcome = new CaseOutcome();
        outcome.setLegalCase(legalCase);
        outcome.setResult(request.result());
        outcome.setSettlementAmount(request.settlementAmount());
        outcome.setGrossRevenue(resolution.grossRevenue());
        outcome.setFirmCut(resolution.firmCut());
        outcome.setNetDistributable(resolution.netDistributable());
        outcome.setRecordedBy(CurrentUser.username());
        CaseOutcome saved = repository.save(outcome);

        legalCase.setFinanciallyResolved(true);

        eventPublisher.publishEvent(new CaseOutcomeRecordedEvent(caseId, saved.getResult(),
                resolution.grossRevenue(), resolution.firmCut(), resolution.netDistributable(), saved.getRecordedBy()));
        return mapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CaseOutcomeResponse getByCase(Long caseId) {
        CaseOutcome outcome = repository.findByLegalCaseId(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("No outcome recorded for case " + caseId));
        return mapper.toResponse(outcome);
    }
}
