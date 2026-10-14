package com.krishna.briefcase.billing;

import com.krishna.briefcase.billing.dto.FeeDistributionResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FeeDistributionService {

    private final FeeDistributionRepository repository;

    public FeeDistributionService(FeeDistributionRepository repository) {
        this.repository = repository;
    }

    public List<FeeDistributionResponse> findByCase(Long caseId) {
        return repository.findByLegalCaseId(caseId).stream().map(this::toResponse).toList();
    }

    private FeeDistributionResponse toResponse(FeeDistribution distribution) {
        return new FeeDistributionResponse(
                distribution.getId(),
                distribution.getLegalCase().getId(),
                distribution.getAttorney().getId(),
                distribution.getAttorney().getFirstName() + " " + distribution.getAttorney().getLastName(),
                distribution.getShareType(),
                distribution.getAmount(),
                distribution.getCreatedAt()
        );
    }
}
