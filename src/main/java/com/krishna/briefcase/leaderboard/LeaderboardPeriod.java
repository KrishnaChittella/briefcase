package com.krishna.briefcase.leaderboard;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

public enum LeaderboardPeriod {
    MONTH,
    QUARTER,
    YEAR,
    ALL_TIME;

    /** Start of the current period, in UTC. ALL_TIME returns the epoch (no lower bound). */
    public Instant startInstant() {
        if (this == ALL_TIME) {
            return Instant.EPOCH;
        }
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate start = switch (this) {
            case MONTH -> today.withDayOfMonth(1);
            case QUARTER -> today.withMonth(((today.getMonthValue() - 1) / 3) * 3 + 1).withDayOfMonth(1);
            case YEAR -> today.withDayOfYear(1);
            case ALL_TIME -> throw new IllegalStateException("unreachable");
        };
        return start.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
