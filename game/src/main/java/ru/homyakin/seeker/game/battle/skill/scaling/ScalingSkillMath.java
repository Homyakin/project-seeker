package ru.homyakin.seeker.game.battle.skill.scaling;

import java.math.BigInteger;

public final class ScalingSkillMath {
    public static final int PROBABILITY_BASIS = 10_000;
    private static final int MAX_EFFECTIVE_POINTS = 8;

    private ScalingSkillMath() {
    }

    public static int effectivePoints(int points) {
        return Math.min(Math.max(points, 0), MAX_EFFECTIVE_POINTS);
    }

    public static int multiplierNumerator(int points) {
        return switch (effectivePoints(points)) {
            case 0 -> 0;
            case 1 -> 8;
            case 2 -> 12;
            case 3 -> 15;
            case 4 -> 18;
            case 5 -> 21;
            case 6 -> 24;
            case 7 -> 27;
            case 8 -> 30;
            default -> throw new IllegalStateException("Unexpected effective points");
        };
    }

    public static NonNegativeRational multiplier(int points) {
        return NonNegativeRational.of(multiplierNumerator(points), 20);
    }

    public static BigInteger roundHalfUp(BigInteger numerator, BigInteger denominator) {
        return new NonNegativeRational(numerator, denominator).roundHalfUp();
    }

    public static int roundHalfUpToInt(NonNegativeRational value) {
        return value.roundHalfUp().intValueExact();
    }

    public static int probabilityThresholdFromPercent(NonNegativeRational percent) {
        if (percent.compareTo(NonNegativeRational.of(100)) > 0) {
            throw new IllegalArgumentException("Probability percent must not exceed 100: " + percent);
        }
        return percent.multiply(100).integerValueExact().intValueExact();
    }

    public static boolean probabilityRollSucceeds(int threshold, int roll) {
        if (threshold < 0 || threshold > PROBABILITY_BASIS) {
            throw new IllegalArgumentException("Probability threshold must be in [0, 10000]: " + threshold);
        }
        if (roll < 1 || roll > PROBABILITY_BASIS) {
            throw new IllegalArgumentException("Probability roll must be in [1, 10000]: " + roll);
        }
        return roll <= threshold;
    }
}
