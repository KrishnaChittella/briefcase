package com.krishna.briefcase.billing;

import com.krishna.briefcase.cases.BillingType;
import com.krishna.briefcase.cases.LegalCase;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

/** Revenue = the flat fee agreed with the client up front. Hours and settlement are not used. */
@Component
public class FlatFeeBillingStrategy implements BillingCalculationStrategy {

    @Override
    public BillingType supports() {
        return BillingType.FLAT_FEE;
    }

    @Override
    public BigDecimal calculateRevenue(LegalCase legalCase, BigDecimal settlementAmount) {
        BigDecimal fee = legalCase.getFlatFee();
        if (fee == null) {
            throw new IllegalArgumentException("Case " + legalCase.getId() + " has no flatFee set");
        }
        return fee.setScale(2, RoundingMode.HALF_UP);
    }
}
