package com.krishna.briefcase.billing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeeDistributionRepository extends JpaRepository<FeeDistribution, Long> {

    List<FeeDistribution> findByLegalCaseId(Long caseId);

    List<FeeDistribution> findByAttorneyId(Long attorneyId);
}
