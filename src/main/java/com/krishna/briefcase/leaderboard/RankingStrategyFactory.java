package com.krishna.briefcase.leaderboard;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Factory: picks the {@link RankingStrategy} bean matching the requested {@link RankingMetric}. */
@Component
public class RankingStrategyFactory {

    private final Map<RankingMetric, RankingStrategy> strategiesByMetric;

    public RankingStrategyFactory(List<RankingStrategy> strategies) {
        this.strategiesByMetric = strategies.stream()
                .collect(Collectors.toMap(RankingStrategy::supports, Function.identity()));
    }

    public RankingStrategy get(RankingMetric metric) {
        RankingStrategy strategy = strategiesByMetric.get(metric);
        if (strategy == null) {
            throw new IllegalStateException("No RankingStrategy registered for " + metric);
        }
        return strategy;
    }
}
