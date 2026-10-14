package com.krishna.briefcase.leaderboard.dto;

import java.math.BigDecimal;

public record AttorneyRankingResponse(int rank, Long attorneyId, String attorneyName, BigDecimal score) {
}
