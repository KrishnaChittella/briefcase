package com.krishna.briefcase.leaderboard;

import java.math.BigDecimal;

/** Unranked score for one attorney; the caller sorts and assigns positions. */
public record AttorneyRanking(Long attorneyId, String attorneyName, BigDecimal score) {
}
