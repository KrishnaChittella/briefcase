package com.krishna.briefcase.billing;

import com.krishna.briefcase.cases.CaseAssignment;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/** Strategy: divides the net distributable amount among a case's assigned attorneys. */
public interface FeeSplitStrategy {

    /** Keyed by attorney id. Implementations must ensure shares sum exactly to netDistributable. */
    Map<Long, FeeShare> split(BigDecimal netDistributable, List<CaseAssignment> assignments);

    record FeeShare(FeeShareType type, BigDecimal amount) {
    }
}
