package ru.homyakin.seeker.game.item.storm;

import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public class StormEnhanceConfigTest {

    @Test
    void Given_DefaultConfig_When_CostForLevel_Then_UsesExactSlotCoefficients() {
        final var config = new StormEnhanceConfig();

        Assertions.assertEquals(40, config.costForLevel(0, Set.of(PersonageSlot.MAIN_HAND)).value());
        Assertions.assertEquals(13, config.costForLevel(0, Set.of(PersonageSlot.OFF_HAND)).value());
        Assertions.assertEquals(25, config.costForLevel(0, Set.of(PersonageSlot.BODY)).value());
        Assertions.assertEquals(18, config.costForLevel(0, Set.of(PersonageSlot.PANTS)).value());
        Assertions.assertEquals(10, config.costForLevel(0, Set.of(PersonageSlot.SHOES)).value());
        Assertions.assertEquals(10, config.costForLevel(0, Set.of(PersonageSlot.HELMET)).value());
        Assertions.assertEquals(10, config.costForLevel(0, Set.of(PersonageSlot.GLOVES)).value());
        Assertions.assertEquals(
            53,
            config.costForLevel(0, Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND)).value()
        );
        Assertions.assertEquals(16, config.costForLevel(2, Set.of(PersonageSlot.SHOES)).value());
    }

    @Test
    void Given_HandPair_When_CostForLevel_Then_DoesNotMultiplyBySlotCountAgain() {
        final var config = new StormEnhanceConfig();

        final var twoHanded = config.costForLevel(
            1,
            Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND)
        ).value();
        final var separateHands = config.costForLevel(1, Set.of(PersonageSlot.MAIN_HAND)).value()
            + config.costForLevel(1, Set.of(PersonageSlot.OFF_HAND)).value();

        Assertions.assertEquals(66, twoHanded);
        Assertions.assertEquals(separateHands, twoHanded);
    }

    @Test
    void Given_DefaultConfig_When_Probabilities_Then_MatchTargets() {
        final var config = new StormEnhanceConfig();

        Assertions.assertEquals(new StormEnhanceProbabilities(100, 0, 0), config.probabilitiesForLevel(0));
        Assertions.assertEquals(new StormEnhanceProbabilities(37, 50, 13), config.probabilitiesForLevel(6));
        Assertions.assertEquals(new StormEnhanceProbabilities(20, 60, 20), config.probabilitiesForLevel(8));
        Assertions.assertEquals(new StormEnhanceProbabilities(10, 63, 27), config.probabilitiesForLevel(10));
        Assertions.assertEquals(new StormEnhanceProbabilities(8, 36, 56), config.probabilitiesForLevel(12));
        Assertions.assertEquals(new StormEnhanceProbabilities(1, 0, 99), config.probabilitiesForLevel(19));
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
    void Given_UnchangedOutcomeCurve_When_ExpectedAttempts_Then_MatchesApprovedCheckpoints() {
        final var config = new StormEnhanceConfig();

        Assertions.assertAll(
            () -> Assertions.assertEquals(3.50, expectedAttempts(config, 3), 0.01),
            () -> Assertions.assertEquals(9.40, expectedAttempts(config, 6), 0.01),
            () -> Assertions.assertEquals(54.60, expectedAttempts(config, 10), 0.01)
        );
    }

    @Test
    void Given_DefaultConfig_When_MaxPriceLevel_Then_UsesAllKnownSlotsAndRejectsNextAttempt() {
        final var config = new StormEnhanceConfig();

        Assertions.assertEquals(75, config.maxPriceSupportedStateLevel());
        Assertions.assertEquals(
            1_854_603_075,
            config.costForLevel(74, EnumSet.allOf(PersonageSlot.class)).value()
        );
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> config.costForLevel(75, Set.of(PersonageSlot.SHOES))
        );
    }

    @Test
    void Given_NegativeLevel_When_CostForLevel_Then_Throws() {
        final var config = new StormEnhanceConfig();
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> config.costForLevel(-1, Set.of(PersonageSlot.SHOES))
        );
        Assertions.assertThrows(IllegalArgumentException.class, () -> config.costForLevel(0, Set.of()));
    }

    @Test
    void Given_InvalidRationalMultiplier_When_CostForLevel_Then_Throws() {
        final var config = new StormEnhanceConfig();
        config.setCostMultiplierNumerator(4);

        Assertions.assertThrows(
            IllegalStateException.class,
            () -> config.costForLevel(0, Set.of(PersonageSlot.SHOES))
        );
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

    private static double expectedAttempts(StormEnhanceConfig config, int targetLevel) {
        var previousStep = 0.0;
        var result = 0.0;
        for (int level = 0; level < targetLevel; level++) {
            final var probabilities = config.probabilitiesForLevel(level);
            final var currentStep = (100 + probabilities.rollbackPercent() * previousStep)
                / probabilities.successPercent();
            result += currentStep;
            previousStep = currentStep;
        }
        return result;
    }
}
