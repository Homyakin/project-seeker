package ru.homyakin.seeker.game.event.anomaly.entity;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * GvG search scoring: {@code score = |ratingDiff| + sum(recentMeetPenalties)}.
 * Target is eligible only if {@code |ratingDiff| <= maxAllowedDiff(searchAge)}.
 */
public final class AnomalyGvgMatchRules {
    private static final int RATING_DIFF_AT_MIN_SEARCH = 50;
    private static final int RATING_DIFF_AT_MAX_SEARCH = 1000;

    private AnomalyGvgMatchRules() {
    }

    /**
     * Max |rating| difference grows linearly from {@value RATING_DIFF_AT_MIN_SEARCH}
     * at {@code minSearch} to {@value RATING_DIFF_AT_MAX_SEARCH} at {@code maxSearch}.
     */
    public static int maxAllowedRatingDiff(
        Duration searchAge,
        Duration minSearch,
        Duration maxSearch
    ) {
        if (searchAge.compareTo(minSearch) <= 0) {
            return RATING_DIFF_AT_MIN_SEARCH;
        }
        if (searchAge.compareTo(maxSearch) >= 0) {
            return RATING_DIFF_AT_MAX_SEARCH;
        }
        final long spanSeconds = maxSearch.minus(minSearch).toSeconds();
        if (spanSeconds <= 0) {
            return RATING_DIFF_AT_MAX_SEARCH;
        }
        final long ageIntoWindow = searchAge.minus(minSearch).toSeconds();
        final double progress = (double) ageIntoWindow / spanSeconds;
        return (int) Math.round(
            RATING_DIFF_AT_MIN_SEARCH
                + progress * (RATING_DIFF_AT_MAX_SEARCH - RATING_DIFF_AT_MIN_SEARCH)
        );
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
