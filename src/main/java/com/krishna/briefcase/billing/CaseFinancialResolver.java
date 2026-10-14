package com.krishna.briefcase.billing;

import com.krishna.briefcase.attorney.Attorney;
import com.krishna.briefcase.cases.CaseAssignment;
import com.krishna.briefcase.cases.CaseAssignmentRepository;
import com.krishna.briefcase.cases.LegalCase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Template Method: the fixed skeleton for resolving a case financially — compute revenue,
 * apply the firm's cut, split the remainder among attorneys, persist. Individual steps delegate
 * to the {@link BillingCalculationStrategy}/{@link FeeSplitStrategy} beans so the swappable
 * rules live there, not here.
 *
 * <p>Deliberately has no {@code @Transactional} of its own: it always runs inside the caller's
 * transaction (see {@code CaseOutcomeService.recordOutcome}), which is what lets {@link #resolve}
 * stay a genuine non-overridable {@code final} skeleton — a method carrying its own
 * {@code @Transactional} would need proxying, and Spring's CGLIB proxies silently can't advise
 * {@code final} methods.
 */
@Service
public class CaseFinancialResolver {

    private final BillingStrategyFactory billingStrategyFactory;
    private final FeeSplitStrategy feeSplitStrategy;
    private final BillingProperties billingProperties;
    private final CaseAssignmentRepository caseAssignmentRepository;
    private final FeeDistributionRepository feeDistributionRepository;

    public CaseFinancialResolver(BillingStrategyFactory billingStrategyFactory,
                                 FeeSplitStrategy feeSplitStrategy,
                                 BillingProperties billingProperties,
                                 CaseAssignmentRepository caseAssignmentRepository,
                                 FeeDistributionRepository feeDistributionRepository) {
        this.billingStrategyFactory = billingStrategyFactory;
        this.feeSplitStrategy = feeSplitStrategy;
        this.billingProperties = billingProperties;
        this.caseAssignmentRepository = caseAssignmentRepository;
        this.feeDistributionRepository = feeDistributionRepository;
    }

    public final Resolution resolve(LegalCase legalCase, BigDecimal settlementAmount) {
        BigDecimal grossRevenue = computeRevenue(legalCase, settlementAmount);
        BigDecimal firmCut = applyFirmCut(grossRevenue);
        BigDecimal netDistributable = grossRevenue.subtract(firmCut);

        List<CaseAssignment> assignments = caseAssignmentRepository.findByLegalCaseId(legalCase.getId());
        Map<Long, FeeSplitStrategy.FeeShare> shares = splitAmongAttorneys(netDistributable, assignments);
        List<FeeDistribution> distributions = persist(legalCase, assignments, shares);

        return new Resolution(grossRevenue, firmCut, netDistributable, distributions);
    }

    protected BigDecimal computeRevenue(LegalCase legalCase, BigDecimal settlementAmount) {
        return billingStrategyFactory.get(legalCase.getBillingType()).calculateRevenue(legalCase, settlementAmount);
    }

    protected BigDecimal applyFirmCut(BigDecimal grossRevenue) {
        return grossRevenue.multiply(billingProperties.firmCutRate()).setScale(2, RoundingMode.HALF_UP);
    }

    protected Map<Long, FeeSplitStrategy.FeeShare> splitAmongAttorneys(BigDecimal netDistributable,
                                                                       List<CaseAssignment> assignments) {
        return feeSplitStrategy.split(netDistributable, assignments);
    }

    protected List<FeeDistribution> persist(LegalCase legalCase, List<CaseAssignment> assignments,
                                            Map<Long, FeeSplitStrategy.FeeShare> shares) {
        Map<Long, Attorney> attorneyById = assignments.stream()
                .collect(Collectors.toMap(a -> a.getAttorney().getId(), CaseAssignment::getAttorney,
                        (a, b) -> a, LinkedHashMap::new));

        List<FeeDistribution> distributions = shares.entrySet().stream().map(entry -> {
            FeeDistribution distribution = new FeeDistribution();
            distribution.setLegalCase(legalCase);
            distribution.setAttorney(attorneyById.get(entry.getKey()));
            distribution.setShareType(entry.getValue().type());
            distribution.setAmount(entry.getValue().amount());
            return distribution;
        }).toList();

        return feeDistributionRepository.saveAll(distributions);
    }

    public record Resolution(BigDecimal grossRevenue, BigDecimal firmCut, BigDecimal netDistributable,
                             List<FeeDistribution> distributions) {
    }
}
