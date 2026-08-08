package ru.homyakin.seeker.game.event.anomaly;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgMatchRules;

public class AnomalyGvgMatchRulesTest {
    private static final Duration MIN_SEARCH = Duration.ofHours(1);
    private static final Duration MAX_SEARCH = Duration.ofHours(12);

    @Test
    void maxAllowedRatingDiff_linearFrom50AtMinTo1000AtMax() {
        Assertions.assertEquals(
            50,
            AnomalyGvgMatchRules.maxAllowedRatingDiff(Duration.ZERO, MIN_SEARCH, MAX_SEARCH)
        );
        Assertions.assertEquals(
            50,
            AnomalyGvgMatchRules.maxAllowedRatingDiff(MIN_SEARCH, MIN_SEARCH, MAX_SEARCH)
        );
        Assertions.assertEquals(
            525,
            AnomalyGvgMatchRules.maxAllowedRatingDiff(Duration.ofHours(6).plusMinutes(30), MIN_SEARCH, MAX_SEARCH)
        );
        Assertions.assertEquals(
            1000,
            AnomalyGvgMatchRules.maxAllowedRatingDiff(MAX_SEARCH, MIN_SEARCH, MAX_SEARCH)
        );
        Assertions.assertEquals(
            1000,
            AnomalyGvgMatchRules.maxAllowedRatingDiff(Duration.ofHours(20), MIN_SEARCH, MAX_SEARCH)
        );
    }

    @Test
    void recentMeetPenalty_halvesEachDayFromConfiguredFirstDay() {
        final var now = LocalDateTime.of(2026, 7, 26, 12, 0);
        Assertions.assertEquals(256, AnomalyGvgMatchRules.recentMeetPenalty(now.minusHours(12), now, 256));
        Assertions.assertEquals(128, AnomalyGvgMatchRules.recentMeetPenalty(now.minusDays(1), now, 256));
        Assertions.assertEquals(64, AnomalyGvgMatchRules.recentMeetPenalty(now.minusDays(2), now, 256));
        Assertions.assertEquals(1, AnomalyGvgMatchRules.recentMeetPenalty(now.minusDays(8), now, 256));
        Assertions.assertEquals(0, AnomalyGvgMatchRules.recentMeetPenalty(now.minusDays(9), now, 256));
    }

    @Test
    void score_isRatingDiffPlusSumOfRecentMeetPenalties() {
        final var now = LocalDateTime.of(2026, 7, 26, 12, 0);
        final var recentSum = AnomalyGvgMatchRules.sumRecentMeetPenalties(
            List.of(now.minusHours(1), now.minusDays(1)),
            now,
            256
        );
        Assertions.assertEquals(256 + 128, recentSum);
        Assertions.assertEquals(30 + 256 + 128, AnomalyGvgMatchRules.score(30, recentSum));
    }
}
