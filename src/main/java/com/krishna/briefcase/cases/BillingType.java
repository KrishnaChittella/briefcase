package com.krishna.briefcase.cases;

/**
 * How a case generates revenue. Drives which BillingCalculationStrategy runs.
 */
public enum BillingType {
    /** Revenue = settlement amount × contingency rate. */
    CONTINGENCY,
    /** Revenue = logged hours × hourly rate. */
    HOURLY,
    /** Revenue = an agreed flat fee, regardless of hours or settlement. */
    FLAT_FEE
}
