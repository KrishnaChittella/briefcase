package com.krishna.briefcase.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krishna.briefcase.attorney.Attorney;
import com.krishna.briefcase.cases.AssignmentRole;
import com.krishna.briefcase.cases.CaseAssignment;
import com.krishna.briefcase.common.exception.InvalidCaseStateException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LeadFixedCutFeeSplitStrategyTest {

    // firmCut is already removed before split() runs; leadCutRate=0.40 applies to netDistributable.
    private final LeadFixedCutFeeSplitStrategy strategy =
            new LeadFixedCutFeeSplitStrategy(new BillingProperties(new BigDecimal("0.40"), new BigDecimal("0.40")));

    private static CaseAssignment assignment(long attorneyId, AssignmentRole role) {
        Attorney attorney = new Attorney();
        attorney.setId(attorneyId);
        CaseAssignment assignment = new CaseAssignment();
        assignment.setAttorney(attorney);
        assignment.setAssignmentRole(role);
        return assignment;
    }

    @Test
    void leadGetsFixedCutAndAssociatesSplitTheRest() {
        List<CaseAssignment> assignments = List.of(
                assignment(1L, AssignmentRole.LEAD),
                assignment(2L, AssignmentRole.ASSOCIATE),
                assignment(3L, AssignmentRole.ASSOCIATE),
                assignment(4L, AssignmentRole.PARALEGAL)
        );

        Map<Long, FeeSplitStrategy.FeeShare> shares = strategy.split(new BigDecimal("10000.00"), assignments);

        // Lead: 40% of 10000 = 4000. Remainder 6000 split between 2 associates = 3000 each.
        assertThat(shares.get(1L).type()).isEqualTo(FeeShareType.LEAD_CUT);
        assertThat(shares.get(1L).amount()).isEqualByComparingTo("4000.00");
        assertThat(shares.get(2L).type()).isEqualTo(FeeShareType.EQUAL_SPLIT);
        assertThat(shares.get(2L).amount()).isEqualByComparingTo("3000.00");
        assertThat(shares.get(3L).amount()).isEqualByComparingTo("3000.00");
        assertThat(shares).doesNotContainKey(4L); // paralegal excluded from the pool

        BigDecimal total = shares.values().stream().map(FeeSplitStrategy.FeeShare::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("10000.00");
    }

    @Test
    void soleLeadTakesEverythingWhenNoAssociates() {
        List<CaseAssignment> assignments = List.of(assignment(1L, AssignmentRole.LEAD));

        Map<Long, FeeSplitStrategy.FeeShare> shares = strategy.split(new BigDecimal("5000.00"), assignments);

        assertThat(shares).hasSize(1);
        assertThat(shares.get(1L).type()).isEqualTo(FeeShareType.SOLE_LEAD);
        assertThat(shares.get(1L).amount()).isEqualByComparingTo("5000.00");
    }

    @Test
    void missingLeadThrows() {
        List<CaseAssignment> assignments = List.of(assignment(2L, AssignmentRole.ASSOCIATE));

        assertThatThrownBy(() -> strategy.split(new BigDecimal("1000.00"), assignments))
                .isInstanceOf(InvalidCaseStateException.class);
    }

    @Test
    void unevenSplitStillSumsExactlyToNetDistributable() {
        // 6000 remainder / 3 associates does not divide evenly at 2dp; rounding must reconcile.
        List<CaseAssignment> assignments = List.of(
                assignment(1L, AssignmentRole.LEAD),
                assignment(2L, AssignmentRole.ASSOCIATE),
                assignment(3L, AssignmentRole.ASSOCIATE),
                assignment(4L, AssignmentRole.ASSOCIATE)
        );

        Map<Long, FeeSplitStrategy.FeeShare> shares = strategy.split(new BigDecimal("10000.01"), assignments);

        BigDecimal total = shares.values().stream().map(FeeSplitStrategy.FeeShare::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertThat(total).isEqualByComparingTo("10000.01");
    }
}
