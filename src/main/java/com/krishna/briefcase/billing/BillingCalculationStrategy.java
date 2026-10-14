package com.krishna.briefcase.billing;

import com.krishna.briefcase.cases.BillingType;
import com.krishna.briefcase.cases.LegalCase;
import java.math.BigDecimal;

/** Strategy: computes gross revenue for a resolved case. One implementation per {@link BillingType}. */
public interface BillingCalculationStrategy {

    BillingType supports();

    /**
     * @param settlementAmount only meaningful for contingency billing; hourly billing derives
     *                         revenue from the case's own loggedHours/hourlyRate instead.
     */
    BigDecimal calculateRevenue(LegalCase legalCase, BigDecimal settlementAmount);
}
