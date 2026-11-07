package ru.homyakin.seeker.game.battle.skill.scaling;

import java.math.BigInteger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ScalingSkillMathTest {
    @ParameterizedTest
    @CsvSource({
        "0, 0",
        "1, 8",
        "2, 12",
        "3, 15",
        "4, 18",
        "5, 21",
        "6, 24",
        "7, 27",
        "8, 30",
        "9, 30",
    })
    void mapsPointsToExactMultiplierNumerator(int points, int expected) {
        Assertions.assertEquals(expected, ScalingSkillMath.multiplierNumerator(points));
    }

    @ParameterizedTest
    @CsvSource({
        "-2147483648, 0", "-1, 0", "0, 0",
        "1, 3000", "2, 4000", "3, 5000", "4, 6000",
        "5, 7000", "6, 8000", "7, 9000", "8, 10000",
        "9, 10000", "2147483647, 10000",
    })
    void mapsV3HitAndRunPointsToExactChanceAndClampsTheEffectiveRange(int points, int expected) {
        final int threshold = ScalingSkillMath.hitAndRunV3Chance(points);

        Assertions.assertEquals(expected, threshold);
        if (threshold > 0) {
            Assertions.assertTrue(ScalingSkillMath.probabilityRollSucceeds(threshold, threshold));
        }
        if (threshold < ScalingSkillMath.PROBABILITY_BASIS) {
            Assertions.assertFalse(ScalingSkillMath.probabilityRollSucceeds(threshold, threshold + 1));
        }
    }

    @Test
    void clampsPointsOutsideEffectiveRange() {
        Assertions.assertEquals(0, ScalingSkillMath.effectivePoints(-10));
        Assertions.assertEquals(8, ScalingSkillMath.effectivePoints(Integer.MAX_VALUE));
        Assertions.assertEquals(NonNegativeRational.of(3, 2), ScalingSkillMath.multiplier(8));
    }

    @Test
    void roundsExactHalvesUpWithoutFloatingPoint() {
        Assertions.assertEquals(BigInteger.ZERO, ScalingSkillMath.roundHalfUp(BigInteger.ZERO, BigInteger.TEN));
        Assertions.assertEquals(BigInteger.ONE, ScalingSkillMath.roundHalfUp(BigInteger.ONE, BigInteger.TWO));
        Assertions.assertEquals(BigInteger.ONE, ScalingSkillMath.roundHalfUp(
            BigInteger.valueOf(149),
            BigInteger.valueOf(100)
        ));
        Assertions.assertEquals(BigInteger.TWO, ScalingSkillMath.roundHalfUp(
            BigInteger.valueOf(150),
            BigInteger.valueOf(100)
        ));
    }

    @Test
    void convertsExactHundredthsOfPercentToProbabilityBasis() {
        Assertions.assertEquals(
            3_750,
            ScalingSkillMath.probabilityThresholdFromPercent(NonNegativeRational.of(75, 2))
        );
        Assertions.assertEquals(
            10_000,
            ScalingSkillMath.probabilityThresholdFromPercent(NonNegativeRational.of(100))
        );
        Assertions.assertEquals(
            0,
            ScalingSkillMath.probabilityThresholdFromPercent(NonNegativeRational.ZERO)
        );
    }

    @Test
    void rejectsProbabilityThatCannotBeStoredInBasisOrExceedsOneHundredPercent() {
        Assertions.assertThrows(
            ArithmeticException.class,
            () -> ScalingSkillMath.probabilityThresholdFromPercent(NonNegativeRational.of(1, 3))
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ScalingSkillMath.probabilityThresholdFromPercent(NonNegativeRational.of(101))
        );
    }

    @Test
    void probabilityRollUsesInclusiveOneToTenThousand() {
        Assertions.assertTrue(ScalingSkillMath.probabilityRollSucceeds(3_750, 3_750));
        Assertions.assertFalse(ScalingSkillMath.probabilityRollSucceeds(3_750, 3_751));
        Assertions.assertFalse(ScalingSkillMath.probabilityRollSucceeds(0, 1));
        Assertions.assertTrue(ScalingSkillMath.probabilityRollSucceeds(10_000, 10_000));
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ScalingSkillMath.probabilityRollSucceeds(10_001, 1)
        );
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> ScalingSkillMath.probabilityRollSucceeds(1, 0)
        );
    }

    @Test
    void intRoundingReportsOverflow() {
        final var tooLarge = new NonNegativeRational(
            BigInteger.valueOf(Integer.MAX_VALUE).add(BigInteger.ONE),
            BigInteger.ONE
        );

        Assertions.assertThrows(ArithmeticException.class, () -> ScalingSkillMath.roundHalfUpToInt(tooLarge));
    }
}
