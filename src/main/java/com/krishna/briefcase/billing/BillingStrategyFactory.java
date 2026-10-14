package com.krishna.briefcase.billing;

import com.krishna.briefcase.cases.BillingType;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Factory: picks the {@link BillingCalculationStrategy} bean matching a case's {@link BillingType}. */
@Component
public class BillingStrategyFactory {

    private final Map<BillingType, BillingCalculationStrategy> strategiesByType;

    public BillingStrategyFactory(List<BillingCalculationStrategy> strategies) {
        this.strategiesByType = strategies.stream()
                .collect(Collectors.toMap(BillingCalculationStrategy::supports, Function.identity()));
    }

    public BillingCalculationStrategy get(BillingType billingType) {
        BillingCalculationStrategy strategy = strategiesByType.get(billingType);
        if (strategy == null) {
            throw new IllegalStateException("No BillingCalculationStrategy registered for " + billingType);
        }
        return strategy;
    }
}
