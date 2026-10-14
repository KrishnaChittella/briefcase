package com.krishna.briefcase.leaderboard;

import com.krishna.briefcase.outcome.CaseResult;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Ranks attorneys by win rate (WON / total resolved cases they were LEAD or ASSOCIATE on), as a percentage. */
@Component
public class WinRateRankingStrategy implements RankingStrategy {

    private final LeaderboardRepository repository;
    private final AttorneyNameLookup nameLookup;

    public WinRateRankingStrategy(LeaderboardRepository repository, AttorneyNameLookup nameLookup) {
        this.repository = repository;
        this.nameLookup = nameLookup;
    }

    @Override
    public RankingMetric supports() {
        return RankingMetric.WIN_RATE;
    }

    @Override
    public List<AttorneyRanking> rank(Instant periodStart) {
        List<LeaderboardRepository.AttorneyResult> results = repository.resultsSince(periodStart);
        Map<Long, List<CaseResult>> byAttorney = results.stream()
                .collect(Collectors.groupingBy(LeaderboardRepository.AttorneyResult::getAttorneyId,
                        Collectors.mapping(LeaderboardRepository.AttorneyResult::getResult, Collectors.toList())));
        Map<Long, String> names = nameLookup.namesFor(byAttorney.keySet());

        return byAttorney.entrySet().stream()
                .map(entry -> {
                    long total = entry.getValue().size();
                    long wins = entry.getValue().stream().filter(r -> r == CaseResult.WON).count();
                    BigDecimal rate = BigDecimal.valueOf(wins * 100)
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
                    return new AttorneyRanking(entry.getKey(), names.getOrDefault(entry.getKey(), "Unknown"), rate);
                })
                .sorted(Comparator.comparing(AttorneyRanking::score).reversed())
                .toList();
    }
}
