package ru.homyakin.seeker.game.item.storm;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class StormEnhanceConfigTest {

    @Test
    void Given_DefaultConfig_When_CostForLevel_Then_ExponentialTimesSlots() {
        final var config = new StormEnhanceConfig();

        Assertions.assertEquals(1, config.costForLevel(0, 1).value());
        Assertions.assertEquals(2, config.costForLevel(1, 1).value());
        Assertions.assertEquals(4, config.costForLevel(2, 1).value());
        Assertions.assertEquals(8, config.costForLevel(3, 1).value());
        Assertions.assertEquals(16, config.costForLevel(4, 1).value());

        Assertions.assertEquals(2, config.costForLevel(0, 2).value());
        Assertions.assertEquals(4, config.costForLevel(1, 2).value());
        Assertions.assertEquals(32, config.costForLevel(4, 2).value());
    }

    @Test
    void Given_DefaultConfig_When_Probabilities_Then_MatchTargets() {
        final var config = new StormEnhanceConfig();

        Assertions.assertEquals(new StormEnhanceProbabilities(100, 0, 0), config.probabilitiesForLevel(0));
        Assertions.assertEquals(new StormEnhanceProbabilities(37, 50, 13), config.probabilitiesForLevel(6));
        Assertions.assertEquals(new StormEnhanceProbabilities(10, 63, 27), config.probabilitiesForLevel(10));
    }

    @Test
    void Given_DefaultConfig_When_ProbabilitiesAcrossLevels_Then_ShapesHold() {
        final var config = new StormEnhanceConfig();
        final var peak = config.failurePeakLevel();
        final var rollbackFrom = config.rollbackFromLevel();

        StormEnhanceProbabilities previous = null;
        for (int level = 0; level <= 20; level++) {
            final var current = config.probabilitiesForLevel(level);
            Assertions.assertEquals(
                100,
                current.successPercent() + current.failurePercent() + current.rollbackPercent(),
                "level " + level
            );

            if (level < rollbackFrom) {
                Assertions.assertEquals(0, current.rollbackPercent(), "level " + level);
            }
            if (previous != null) {
                if (previous.successPercent() > 1) {
                    Assertions.assertTrue(
                        current.successPercent() < previous.successPercent(),
                        "success should decrease at level " + level
                            + ": " + previous.successPercent() + " -> " + current.successPercent()
                    );
                } else {
                    Assertions.assertTrue(
                        current.successPercent() <= previous.successPercent(),
                        "success should not increase at level " + level
                    );
                }

                if (level <= peak) {
                    Assertions.assertTrue(
                        current.failurePercent() > previous.failurePercent(),
                        "failure should grow until peak at level " + level
                            + ": " + previous.failurePercent() + " -> " + current.failurePercent()
                    );
                } else {
                    Assertions.assertTrue(
                        current.failurePercent() <= previous.failurePercent(),
                        "failure should not grow after peak at level " + level
                            + ": " + previous.failurePercent() + " -> " + current.failurePercent()
                    );
                }

                if (level > rollbackFrom) {
                    Assertions.assertTrue(
                        current.rollbackPercent() >= previous.rollbackPercent(),
                        "rollback should grow after level " + rollbackFrom + " at level " + level
                            + ": " + previous.rollbackPercent() + " -> " + current.rollbackPercent()
                    );
                }
            }
            previous = current;
        }
    }

    @Test
    void Given_HighLevel_When_CostForLevel_Then_CapsAtMaxInt() {
        final var config = new StormEnhanceConfig();
        Assertions.assertEquals(Integer.MAX_VALUE, config.costForLevel(40, 1).value());
    }

    @Test
    void Given_NegativeLevel_When_CostForLevel_Then_Throws() {
        final var config = new StormEnhanceConfig();
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.costForLevel(-1, 1));
    }

    @Test
    void Given_EnhanceLevel_When_ApplyBonus_Then_UsesBonusPercentPerLevel() {
        final var config = new StormEnhanceConfig();
        try {
            Assertions.assertEquals(5, config.bonusPercentPerLevel());
            Assertions.assertEquals(110, config.applyBonus(100, 2));

            config.setBonusPercentPerLevel(10);
            Assertions.assertEquals(120, config.applyBonus(100, 2));
            Assertions.assertEquals(120, StormEnhanceConfig.applyConfiguredBonus(100, 2));
        } finally {
            config.setBonusPercentPerLevel(5);
        }
    }
}
