package com.krishna.briefcase.leaderboard;

import java.time.Instant;
import java.util.List;

/** Strategy: ranks attorneys by one metric over cases active since {@code periodStart}. */
public interface RankingStrategy {

    RankingMetric supports();

    /** Descending by score. */
    List<AttorneyRanking> rank(Instant periodStart);
}
