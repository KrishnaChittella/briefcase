package com.krishna.briefcase.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krishna.briefcase.cases.BillingType;
import com.krishna.briefcase.cases.LegalCase;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FlatFeeBillingStrategyTest {

    private final FlatFeeBillingStrategy strategy = new FlatFeeBillingStrategy();

    @Test
    void supportsFlatFeeBillingType() {
        assertThat(strategy.supports()).isEqualTo(BillingType.FLAT_FEE);
    }

    @Test
    void revenueIsTheFlatFee() {
        LegalCase legalCase = new LegalCase();
        legalCase.setFlatFee(new BigDecimal("2500"));

        assertThat(strategy.calculateRevenue(legalCase, null)).isEqualByComparingTo("2500.00");
    }

    @Test
    void hoursAndSettlementAreIgnored() {
        LegalCase legalCase = new LegalCase();
        legalCase.setFlatFee(new BigDecimal("2500"));
        legalCase.setHourlyRate(new BigDecimal("350"));
        legalCase.setLoggedHours(new BigDecimal("40"));

        assertThat(strategy.calculateRevenue(legalCase, new BigDecimal("999999"))).isEqualByComparingTo("2500.00");
    }

    @Test
    void missingFlatFeeThrows() {
        assertThatThrownBy(() -> strategy.calculateRevenue(new LegalCase(), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
