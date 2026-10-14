package com.krishna.briefcase.billing;

import com.krishna.briefcase.cases.BillingType;
import com.krishna.briefcase.cases.LegalCase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Revenue = settlement amount x the case's contingency rate. */
@Component
public class ContingencyBillingStrategy implements BillingCalculationStrategy {

    @Override
    public BillingType supports() {
        return BillingType.CONTINGENCY;
    }

    @Override
    public BigDecimal calculateRevenue(LegalCase legalCase, BigDecimal settlementAmount) {
        if (settlementAmount == null) {
            throw new IllegalArgumentException("settlementAmount is required to resolve a CONTINGENCY case");
        }
        BigDecimal rate = legalCase.getContingencyRate();
        if (rate == null) {
            throw new IllegalArgumentException("Case " + legalCase.getId() + " has no contingencyRate set");
        }
        return settlementAmount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }
}
