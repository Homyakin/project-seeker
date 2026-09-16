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
