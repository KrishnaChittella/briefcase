package com.krishna.briefcase.cases;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CaseRepository extends JpaRepository<LegalCase, Long>,
        JpaSpecificationExecutor<LegalCase> {

    Optional<LegalCase> findByCaseNumber(String caseNumber);

    boolean existsByCaseNumber(String caseNumber);
}
