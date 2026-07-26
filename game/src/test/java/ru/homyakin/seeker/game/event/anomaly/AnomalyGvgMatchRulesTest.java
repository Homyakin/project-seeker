package ru.homyakin.seeker.game.event.anomaly;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.event.anomaly.entity.AnomalyGvgMatchRules;

public class AnomalyGvgMatchRulesTest {

    @Test
    void maxAllowedRatingDiff_onePer3_6SecondsWithMin10() {
        Assertions.assertEquals(10, AnomalyGvgMatchRules.maxAllowedRatingDiff(Duration.ZERO));
        Assertions.assertEquals(10, AnomalyGvgMatchRules.maxAllowedRatingDiff(Duration.ofSeconds(36)));
        Assertions.assertEquals(11, AnomalyGvgMatchRules.maxAllowedRatingDiff(Duration.ofSeconds(40)));
        Assertions.assertEquals(100, AnomalyGvgMatchRules.maxAllowedRatingDiff(Duration.ofSeconds(360)));
        Assertions.assertEquals(1000, AnomalyGvgMatchRules.maxAllowedRatingDiff(Duration.ofHours(1)));
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
