package ru.homyakin.seeker.game.battle.targeting;

public final class TargetingMath {
    private TargetingMath() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Advantage of {@code value} over the worst candidate, scaled by {@code max}.
     * Used by {@link TargetingTactic#EXPLOIT_WEAKNESS}.
     */
    public static double relativeAdvantageScore(double value, double min, double max) {
        if (max <= 0.0 || max == min) {
            return 0.0;
        }
        return clamp((value - min) / max, 0.0, 1.0);
    }

    /**
     * Adds a local tactical bonus on top of {@code baseThreat}:
     * {@code weight = baseThreat + referenceThreat * bonusFactor * score}.
     * Never reduces weight below {@code baseThreat}. Does not mutate threat.
     */
    public static int targetingWeight(
        int baseThreat,
        double score,
        int referenceThreat,
        double bonusFactor
    ) {
        final var safeBase = Math.max(1, baseThreat);
        final var safeReference = Math.max(1, referenceThreat);
        final var weight = safeBase
            + safeReference * bonusFactor * clamp(score, 0.0, 1.0);
        return Math.max(1, (int) Math.round(weight));
    }
}
