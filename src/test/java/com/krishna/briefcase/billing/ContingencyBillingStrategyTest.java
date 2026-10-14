package com.krishna.briefcase.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krishna.briefcase.cases.BillingType;
import com.krishna.briefcase.cases.LegalCase;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ContingencyBillingStrategyTest {

    private final ContingencyBillingStrategy strategy = new ContingencyBillingStrategy();

    @Test
    void supportsContingencyBillingType() {
        assertThat(strategy.supports()).isEqualTo(BillingType.CONTINGENCY);
    }

    @Test
    void revenueIsSettlementTimesContingencyRate() {
        LegalCase legalCase = new LegalCase();
        legalCase.setContingencyRate(new BigDecimal("0.33"));

        BigDecimal revenue = strategy.calculateRevenue(legalCase, new BigDecimal("100000"));

        assertThat(revenue).isEqualByComparingTo("33000.00");
    }

    @Test
    void missingSettlementAmountThrows() {
        LegalCase legalCase = new LegalCase();
        legalCase.setContingencyRate(new BigDecimal("0.33"));

        assertThatThrownBy(() -> strategy.calculateRevenue(legalCase, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("settlementAmount");
    }

    @Test
    void missingContingencyRateThrows() {
        LegalCase legalCase = new LegalCase();

        assertThatThrownBy(() -> strategy.calculateRevenue(legalCase, new BigDecimal("100000")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("contingencyRate");
    }
}
