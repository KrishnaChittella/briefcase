package com.krishna.briefcase.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.krishna.briefcase.cases.BillingType;
import com.krishna.briefcase.cases.LegalCase;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class HourlyBillingStrategyTest {

    private final HourlyBillingStrategy strategy = new HourlyBillingStrategy();

    @Test
    void supportsHourlyBillingType() {
        assertThat(strategy.supports()).isEqualTo(BillingType.HOURLY);
    }

    @Test
    void revenueIsHourlyRateTimesLoggedHours() {
        LegalCase legalCase = new LegalCase();
        legalCase.setHourlyRate(new BigDecimal("350"));
        legalCase.setLoggedHours(new BigDecimal("12.5"));

        BigDecimal revenue = strategy.calculateRevenue(legalCase, null);

        assertThat(revenue).isEqualByComparingTo("4375.00");
    }

    @Test
    void settlementAmountIsIgnored() {
        LegalCase legalCase = new LegalCase();
        legalCase.setHourlyRate(new BigDecimal("350"));
        legalCase.setLoggedHours(new BigDecimal("10"));

        BigDecimal revenue = strategy.calculateRevenue(legalCase, new BigDecimal("999999"));

        assertThat(revenue).isEqualByComparingTo("3500.00");
    }

    @Test
    void missingLoggedHoursThrows() {
        LegalCase legalCase = new LegalCase();
        legalCase.setHourlyRate(new BigDecimal("350"));

        assertThatThrownBy(() -> strategy.calculateRevenue(legalCase, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
