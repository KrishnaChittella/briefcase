package com.krishna.briefcase.leaderboard;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Ranks attorneys by count of resolved cases where they were LEAD, since the period start. */
@Component
public class CasesLedRankingStrategy implements RankingStrategy {

    private final LeaderboardRepository repository;
    private final AttorneyNameLookup nameLookup;

    public CasesLedRankingStrategy(LeaderboardRepository repository, AttorneyNameLookup nameLookup) {
        this.repository = repository;
        this.nameLookup = nameLookup;
    }

    @Override
    public RankingMetric supports() {
        return RankingMetric.CASES_LED;
    }

    @Override
    public List<AttorneyRanking> rank(Instant periodStart) {
        List<LeaderboardRepository.AttorneyCount> counts = repository.casesLedSince(periodStart);
        Map<Long, String> names = nameLookup.namesFor(counts.stream().map(LeaderboardRepository.AttorneyCount::getAttorneyId).toList());

        return counts.stream()
                .map(c -> new AttorneyRanking(c.getAttorneyId(),
                        names.getOrDefault(c.getAttorneyId(), "Unknown"), BigDecimal.valueOf(c.getTotal())))
                .sorted(Comparator.comparing(AttorneyRanking::score).reversed())
                .toList();
    }
}
