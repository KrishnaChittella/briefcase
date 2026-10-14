package com.krishna.briefcase.billing;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Binds {@code billing.*} settings. Validated at startup so a typo like {@code 40} instead of
 * {@code 0.40} stops the app from booting instead of paying out nonsense.
 *
 * @param firmCutRate Fraction of gross revenue the firm takes first (e.g. 0.40).
 * @param leadCutRate Fraction of what remains that goes to the LEAD attorney (e.g. 0.40).
 */
@Validated
@ConfigurationProperties(prefix = "billing")
public record BillingProperties(
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal firmCutRate,
        @NotNull @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal leadCutRate
) {
}
