package com.krishna.briefcase.cases;

import com.krishna.briefcase.cases.dto.CaseResponse;
import com.krishna.briefcase.cases.dto.OnboardCaseRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaseOnboardingService {

    private final CaseService caseService;
    private final CaseAssignmentService assignmentService;

    public CaseOnboardingService(CaseService caseService, CaseAssignmentService assignmentService) {
        this.caseService = caseService;
        this.assignmentService = assignmentService;
    }

    /** Wraps CaseBuilder.build() in the single transaction that makes it atomic. */
    @Transactional
    public CaseResponse onboard(OnboardCaseRequest request) {
        CaseBuilder builder = CaseBuilder.newCase(caseService, assignmentService)
                .caseNumber(request.caseNumber())
                .title(request.title())
                .description(request.description())
                .client(request.clientId())
                .billingType(request.billingType())
                .contingencyRate(request.contingencyRate())
                .hourlyRate(request.hourlyRate())
                .loggedHours(request.loggedHours())
                .flatFee(request.flatFee())
                .withLead(request.leadAttorneyId());

        if (request.associateAttorneyIds() != null) {
            request.associateAttorneyIds().forEach(builder::withAssociate);
        }
        if (request.paralegalAttorneyIds() != null) {
            request.paralegalAttorneyIds().forEach(builder::withParalegal);
        }
        return builder.build();
    }
}
