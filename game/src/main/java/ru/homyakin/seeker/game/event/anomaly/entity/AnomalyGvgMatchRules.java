package ru.homyakin.seeker.game.event.anomaly.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GvG search scoring: {@code score = |ratingDiff| + sum(recentMeetPenalties)}.
 * Target is eligible only if {@code |ratingDiff| <= maxAllowedDiff(searchAge)}.
 */
public final class AnomalyGvgMatchRules {
    private static final double SECONDS_PER_RATING_DIFF = 3.6;
    private static final int MIN_ALLOWED_RATING_DIFF = 10;

    private AnomalyGvgMatchRules() {
    }

    /**
     * Max |rating| difference by search age: +1 per 3.6 seconds, minimum 10.
     */
    public static int maxAllowedRatingDiff(Duration searchAge) {
        final long seconds = Math.max(0, searchAge.toSeconds());
        return Math.max(MIN_ALLOWED_RATING_DIFF, (int) (seconds / SECONDS_PER_RATING_DIFF));
    }

    /**
     * Penalty for a single past fight: day 0 (&lt;24h) = {@code firstDayPenalty},
     * each next full day halves until 0.
     */
    public static long recentMeetPenalty(LocalDateTime foughtAt, LocalDateTime now, int firstDayPenalty) {
        if (foughtAt.isAfter(now) || firstDayPenalty <= 0) {
            return 0L;
        }
        final long daysAgo = Duration.between(foughtAt, now).toDays();
        if (daysAgo >= 31) {
            return 0L;
        }
        return firstDayPenalty >> daysAgo;
    }

    public static long sumRecentMeetPenalties(
        List<LocalDateTime> foughtAtList,
        LocalDateTime now,
        int firstDayPenalty
    ) {
        long sum = 0L;
        for (final var foughtAt : foughtAtList) {
            sum += recentMeetPenalty(foughtAt, now, firstDayPenalty);
        }
        return sum;
    }

    public static long score(int ratingDiff, long recentMeetPenaltiesSum) {
        return Math.abs(ratingDiff) + recentMeetPenaltiesSum;
    }
}
