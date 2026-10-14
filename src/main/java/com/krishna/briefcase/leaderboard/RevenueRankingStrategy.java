package com.krishna.briefcase.leaderboard;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Ranks attorneys by total fee-distribution revenue received since the period start. */
@Component
public class RevenueRankingStrategy implements RankingStrategy {

    private final LeaderboardRepository repository;
    private final AttorneyNameLookup nameLookup;

    public RevenueRankingStrategy(LeaderboardRepository repository, AttorneyNameLookup nameLookup) {
        this.repository = repository;
        this.nameLookup = nameLookup;
    }

    @Override
    public RankingMetric supports() {
        return RankingMetric.REVENUE;
    }

    @Override
    public List<AttorneyRanking> rank(Instant periodStart) {
        List<LeaderboardRepository.AttorneyAmount> totals = repository.totalRevenueSince(periodStart);
        Map<Long, String> names = nameLookup.namesFor(totals.stream().map(LeaderboardRepository.AttorneyAmount::getAttorneyId).toList());

        return totals.stream()
                .map(t -> new AttorneyRanking(t.getAttorneyId(),
                        names.getOrDefault(t.getAttorneyId(), "Unknown"), t.getTotal()))
                .sorted(Comparator.comparing(AttorneyRanking::score).reversed())
                .toList();
    }
}
