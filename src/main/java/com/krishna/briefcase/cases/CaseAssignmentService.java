package com.krishna.briefcase.cases;

import com.krishna.briefcase.attorney.Attorney;
import com.krishna.briefcase.attorney.AttorneyService;
import com.krishna.briefcase.cases.dto.CaseAssignmentRequest;
import com.krishna.briefcase.cases.dto.CaseAssignmentResponse;
import com.krishna.briefcase.cases.event.CaseAssignmentChangedEvent;
import com.krishna.briefcase.common.exception.DuplicateResourceException;
import com.krishna.briefcase.common.exception.InvalidCaseStateException;
import com.krishna.briefcase.common.exception.ResourceNotFoundException;
import com.krishna.briefcase.common.security.CurrentUser;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CaseAssignmentService {

    private final CaseAssignmentRepository repository;
    private final CaseService caseService;
    private final AttorneyService attorneyService;
    private final ApplicationEventPublisher eventPublisher;

    public CaseAssignmentService(CaseAssignmentRepository repository, CaseService caseService,
                                 AttorneyService attorneyService, ApplicationEventPublisher eventPublisher) {
        this.repository = repository;
        this.caseService = caseService;
        this.attorneyService = attorneyService;
        this.eventPublisher = eventPublisher;
    }

    public CaseAssignmentResponse assign(Long caseId, CaseAssignmentRequest request) {
        LegalCase legalCase = caseService.getEntity(caseId);
        Attorney attorney = attorneyService.getEntity(request.attorneyId());

        if (repository.existsByLegalCaseIdAndAttorneyId(caseId, request.attorneyId())) {
            throw new DuplicateResourceException(
                    "Attorney " + request.attorneyId() + " is already assigned to case " + caseId);
        }
        if (request.assignmentRole() == AssignmentRole.LEAD
                && repository.existsByLegalCaseIdAndAssignmentRole(caseId, AssignmentRole.LEAD)) {
            throw new InvalidCaseStateException(
                    "Case " + caseId + " already has a LEAD attorney; unassign them first");
        }

        CaseAssignment assignment = new CaseAssignment();
        assignment.setLegalCase(legalCase);
        assignment.setAttorney(attorney);
        assignment.setAssignmentRole(request.assignmentRole());
        CaseAssignment saved = repository.save(assignment);

        eventPublisher.publishEvent(new CaseAssignmentChangedEvent(caseId, request.attorneyId(),
                request.assignmentRole(), CaseAssignmentChangedEvent.ChangeType.ASSIGNED, CurrentUser.username()));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CaseAssignmentResponse> findByCase(Long caseId) {
        return repository.findByLegalCaseId(caseId).stream().map(this::toResponse).toList();
    }

    public void unassign(Long caseId, Long attorneyId) {
        CaseAssignment assignment = repository.findByLegalCaseIdAndAttorneyId(caseId, attorneyId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Attorney " + attorneyId + " is not assigned to case " + caseId));
        AssignmentRole role = assignment.getAssignmentRole();
        repository.delete(assignment);

        eventPublisher.publishEvent(new CaseAssignmentChangedEvent(caseId, attorneyId, role,
                CaseAssignmentChangedEvent.ChangeType.UNASSIGNED, CurrentUser.username()));
    }

    private CaseAssignmentResponse toResponse(CaseAssignment assignment) {
        Attorney attorney = assignment.getAttorney();
        return new CaseAssignmentResponse(
                assignment.getId(),
                assignment.getLegalCase().getId(),
                attorney.getId(),
                attorney.getFirstName() + " " + attorney.getLastName(),
                assignment.getAssignmentRole(),
                assignment.getAssignedAt()
        );
    }
}
