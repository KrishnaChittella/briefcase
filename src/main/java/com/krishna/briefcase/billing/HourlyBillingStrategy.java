package com.krishna.briefcase.billing;

import com.krishna.briefcase.cases.BillingType;
import com.krishna.briefcase.cases.LegalCase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Revenue = the case's logged hours x its hourly rate. Settlement amount is not used. */
@Component
public class HourlyBillingStrategy implements BillingCalculationStrategy {

    @Override
    public BillingType supports() {
        return BillingType.HOURLY;
    }

    @Override
    public BigDecimal calculateRevenue(LegalCase legalCase, BigDecimal settlementAmount) {
        BigDecimal hours = legalCase.getLoggedHours();
        BigDecimal rate = legalCase.getHourlyRate();
        if (hours == null || rate == null) {
            throw new IllegalArgumentException(
                    "Case " + legalCase.getId() + " must have loggedHours and hourlyRate set before resolving");
        }
        return rate.multiply(hours).setScale(2, RoundingMode.HALF_UP);
    }
}
