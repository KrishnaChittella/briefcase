package com.krishna.briefcase.outcome;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaseOutcomeRepository extends JpaRepository<CaseOutcome, Long> {

    Optional<CaseOutcome> findByLegalCaseId(Long caseId);
}
