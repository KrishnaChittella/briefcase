package com.krishna.briefcase.billing;

import com.krishna.briefcase.cases.AssignmentRole;
import com.krishna.briefcase.cases.CaseAssignment;
import com.krishna.briefcase.common.exception.InvalidCaseStateException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * LEAD gets a fixed cut (billing.lead-cut-rate) of the net distributable amount; the remainder
 * is split equally among ASSOCIATE assignees. PARALEGAL assignments are excluded from the pool
 * entirely. If there are no ASSOCIATEs to split the remainder with, the LEAD takes all of it —
 * there being no one else to distribute to.
 */
@Component
public class LeadFixedCutFeeSplitStrategy implements FeeSplitStrategy {

    private final BillingProperties properties;

    public LeadFixedCutFeeSplitStrategy(BillingProperties properties) {
        this.properties = properties;
    }

    @Override
    public Map<Long, FeeShare> split(BigDecimal netDistributable, List<CaseAssignment> assignments) {
        CaseAssignment lead = assignments.stream()
                .filter(a -> a.getAssignmentRole() == AssignmentRole.LEAD)
                .findFirst()
                .orElseThrow(() -> new InvalidCaseStateException(
                        "Case has no LEAD attorney assigned; cannot distribute fees"));

        List<CaseAssignment> associates = assignments.stream()
                .filter(a -> a.getAssignmentRole() == AssignmentRole.ASSOCIATE)
                .toList();

        Map<Long, FeeShare> shares = new LinkedHashMap<>();
        Long leadAttorneyId = lead.getAttorney().getId();

        if (associates.isEmpty()) {
            shares.put(leadAttorneyId, new FeeShare(FeeShareType.SOLE_LEAD, netDistributable));
            return shares;
        }

        BigDecimal leadCut = netDistributable.multiply(properties.leadCutRate()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal remainder = netDistributable.subtract(leadCut);
        BigDecimal perAssociate = remainder.divide(BigDecimal.valueOf(associates.size()), 2, RoundingMode.HALF_UP);

        // Rounding can leave a few cents unaccounted for; fold them into the lead's cut so the
        // sum of all shares always equals netDistributable exactly.
        BigDecimal distributedToAssociates = perAssociate.multiply(BigDecimal.valueOf(associates.size()));
        BigDecimal roundingAdjustment = remainder.subtract(distributedToAssociates);

        shares.put(leadAttorneyId, new FeeShare(FeeShareType.LEAD_CUT, leadCut.add(roundingAdjustment)));
        for (CaseAssignment associate : associates) {
            shares.put(associate.getAttorney().getId(), new FeeShare(FeeShareType.EQUAL_SPLIT, perAssociate));
        }
        return shares;
    }
}
