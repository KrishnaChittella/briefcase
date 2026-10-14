package com.krishna.briefcase.cases;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseAssignmentRepository extends JpaRepository<CaseAssignment, Long> {

    List<CaseAssignment> findByLegalCaseId(Long caseId);

    Optional<CaseAssignment> findByLegalCaseIdAndAttorneyId(Long caseId, Long attorneyId);

    boolean existsByLegalCaseIdAndAttorneyId(Long caseId, Long attorneyId);

    boolean existsByLegalCaseIdAndAssignmentRole(Long caseId, AssignmentRole assignmentRole);

    List<CaseAssignment> findByAttorneyId(Long attorneyId);
}
