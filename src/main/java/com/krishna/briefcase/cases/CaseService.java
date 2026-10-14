package com.krishna.briefcase.cases;

import com.krishna.briefcase.cases.dto.CaseRequest;
import com.krishna.briefcase.cases.dto.CaseResponse;
import com.krishna.briefcase.cases.event.CaseStatusChangedEvent;
import com.krishna.briefcase.client.ClientService;
import com.krishna.briefcase.common.exception.DuplicateResourceException;
import com.krishna.briefcase.common.exception.InvalidCaseStateException;
import com.krishna.briefcase.common.exception.ResourceNotFoundException;
import com.krishna.briefcase.common.security.CurrentUser;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CaseService {

    private final CaseRepository repository;
    private final CaseMapper mapper;
    private final ClientService clientService;
    private final ApplicationEventPublisher eventPublisher;

    public CaseService(CaseRepository repository, CaseMapper mapper, ClientService clientService,
                       ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.mapper = mapper;
        this.clientService = clientService;
        this.eventPublisher = eventPublisher;
    }

    public CaseResponse create(CaseRequest request) {
        if (repository.existsByCaseNumber(request.caseNumber())) {
            throw new DuplicateResourceException("Case already exists with number " + request.caseNumber());
        }
        LegalCase legalCase = new LegalCase();
        legalCase.setCaseNumber(request.caseNumber());
        legalCase.setClient(clientService.getEntity(request.clientId()));
        legalCase.setStatus(CaseStatus.OPEN);
        applyEditableFields(legalCase, request);
        return mapper.toResponse(repository.save(legalCase));
    }

    /** Paginated, filterable case search (Phase 5). Any filter left null is not applied. */
    @Transactional(readOnly = true)
    public Page<CaseResponse> search(CaseStatus status, BillingType billingType, Long clientId,
                                     Long attorneyId, Instant openedFrom, Instant openedTo,
                                     String q, Pageable pageable) {
        Specification<LegalCase> spec = Specification.allOf(
                CaseSpecifications.hasStatus(status),
                CaseSpecifications.hasBillingType(billingType),
                CaseSpecifications.hasClient(clientId),
                CaseSpecifications.assignedToAttorney(attorneyId),
                CaseSpecifications.openedAfter(openedFrom),
                CaseSpecifications.openedBefore(openedTo),
                CaseSpecifications.textSearch(q)
        );
        return repository.findAll(spec, pageable).map(mapper::toResponse);
    }

    @Transactional(readOnly = true)
    public CaseResponse getById(Long id) {
        return mapper.toResponse(getEntity(id));
    }

    @Transactional(readOnly = true)
    public LegalCase getEntity(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Case", id));
    }

    public CaseResponse update(Long id, CaseRequest request) {
        LegalCase legalCase = getEntity(id);
        applyEditableFields(legalCase, request);
        return mapper.toResponse(legalCase);
    }

    /** Advances the case lifecycle; only transitions allowed by {@link CaseStatus#canTransitionTo} succeed. */
    public CaseResponse transitionStatus(Long id, CaseStatus targetStatus) {
        LegalCase legalCase = getEntity(id);
        CaseStatus current = legalCase.getStatus();
        if (!current.canTransitionTo(targetStatus)) {
            throw new InvalidCaseStateException(
                    "Cannot transition case " + id + " from " + current + " to " + targetStatus);
        }
        legalCase.setStatus(targetStatus);
        Instant now = Instant.now();
        if (targetStatus == CaseStatus.RESOLVED) {
            legalCase.setResolvedAt(now);
        } else if (targetStatus == CaseStatus.CLOSED) {
            legalCase.setClosedAt(now);
        }
        eventPublisher.publishEvent(new CaseStatusChangedEvent(id, current, targetStatus, CurrentUser.username()));
        return mapper.toResponse(legalCase);
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of("Case", id);
        }
        repository.deleteById(id);
    }

    /** Fields a client may set on create or edit (caseNumber/client/status handled separately). */
    private void applyEditableFields(LegalCase legalCase, CaseRequest request) {
        validateBillingConfig(request);
        legalCase.setTitle(request.title());
        legalCase.setDescription(request.description());
        legalCase.setBillingType(request.billingType());
        legalCase.setContingencyRate(request.contingencyRate());
        legalCase.setHourlyRate(request.hourlyRate());
        legalCase.setLoggedHours(request.loggedHours());
        legalCase.setFlatFee(request.flatFee());
    }

    /** Each billing type needs its own pricing field: contingency rate, hourly rate, or flat fee. */
    private void validateBillingConfig(CaseRequest request) {
        switch (request.billingType()) {
            case CONTINGENCY -> {
                if (request.contingencyRate() == null) {
                    throw new IllegalArgumentException("contingencyRate is required for CONTINGENCY cases");
                }
            }
            case HOURLY -> {
                if (request.hourlyRate() == null) {
                    throw new IllegalArgumentException("hourlyRate is required for HOURLY cases");
                }
            }
            case FLAT_FEE -> {
                if (request.flatFee() == null) {
                    throw new IllegalArgumentException("flatFee is required for FLAT_FEE cases");
                }
            }
        }
    }
}
