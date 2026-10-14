package com.krishna.briefcase.cases;

import com.krishna.briefcase.cases.dto.CaseAssignmentRequest;
import com.krishna.briefcase.cases.dto.CaseRequest;
import com.krishna.briefcase.cases.dto.CaseResponse;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Builder: fluent construction of a case together with its initial attorney assignments,
 * applied in one {@link #build()} call. A plain object (not a Spring bean) — atomicity is the
 * caller's responsibility (see {@code CaseOnboardingService}, which wraps build() in a single
 * transaction). Delegates to {@link CaseService}/{@link CaseAssignmentService} so existing
 * validation (duplicate case number, one-LEAD-per-case, etc.) isn't duplicated here.
 */
public class CaseBuilder {

    private final CaseService caseService;
    private final CaseAssignmentService assignmentService;
    private final List<CaseAssignmentRequest> assignments = new ArrayList<>();

    private String caseNumber;
    private String title;
    private String description;
    private Long clientId;
    private BillingType billingType;
    private BigDecimal contingencyRate;
    private BigDecimal hourlyRate;
    private BigDecimal loggedHours;
    private BigDecimal flatFee;

    private CaseBuilder(CaseService caseService, CaseAssignmentService assignmentService) {
        this.caseService = caseService;
        this.assignmentService = assignmentService;
    }

    public static CaseBuilder newCase(CaseService caseService, CaseAssignmentService assignmentService) {
        return new CaseBuilder(caseService, assignmentService);
    }

    public CaseBuilder caseNumber(String caseNumber) {
        this.caseNumber = caseNumber;
        return this;
    }

    public CaseBuilder title(String title) {
        this.title = title;
        return this;
    }

    public CaseBuilder description(String description) {
        this.description = description;
        return this;
    }

    public CaseBuilder client(Long clientId) {
        this.clientId = clientId;
        return this;
    }

    public CaseBuilder billingType(BillingType billingType) {
        this.billingType = billingType;
        return this;
    }

    public CaseBuilder contingencyRate(BigDecimal contingencyRate) {
        this.contingencyRate = contingencyRate;
        return this;
    }

    public CaseBuilder hourlyRate(BigDecimal hourlyRate) {
        this.hourlyRate = hourlyRate;
        return this;
    }

    public CaseBuilder loggedHours(BigDecimal loggedHours) {
        this.loggedHours = loggedHours;
        return this;
    }

    public CaseBuilder flatFee(BigDecimal flatFee) {
        this.flatFee = flatFee;
        return this;
    }

    public CaseBuilder withLead(Long attorneyId) {
        assignments.add(new CaseAssignmentRequest(attorneyId, AssignmentRole.LEAD));
        return this;
    }

    public CaseBuilder withAssociate(Long attorneyId) {
        assignments.add(new CaseAssignmentRequest(attorneyId, AssignmentRole.ASSOCIATE));
        return this;
    }

    public CaseBuilder withParalegal(Long attorneyId) {
        assignments.add(new CaseAssignmentRequest(attorneyId, AssignmentRole.PARALEGAL));
        return this;
    }

    public CaseResponse build() {
        CaseRequest request = new CaseRequest(caseNumber, title, description, clientId, billingType,
                contingencyRate, hourlyRate, loggedHours, flatFee);
        CaseResponse created = caseService.create(request);
        assignments.forEach(a -> assignmentService.assign(created.id(), a));
        return caseService.getById(created.id());
    }
}
