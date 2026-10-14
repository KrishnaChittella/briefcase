package com.krishna.briefcase.outcome.dto;

import com.krishna.briefcase.outcome.CaseResult;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Required for CONTINGENCY cases (feeds revenue = settlementAmount x contingencyRate); ignored for HOURLY and FLAT_FEE. */
public record RecordOutcomeRequest(
        @NotNull CaseResult result,
        @DecimalMin("0.0") BigDecimal settlementAmount
) {
}
