package ru.homyakin.seeker.game.battle.targeting;

public final class TargetingMath {
    private TargetingMath() {
    }

    public static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Interpolates from {@code baseThreat} toward {@code referenceThreat * priorityFactor}
     * by {@code score}. Never reduces weight below {@code baseThreat}.
     */
    public static int targetingWeight(
        int baseThreat,
        double score,
        int referenceThreat,
        double priorityFactor
    ) {
        final var safeBase = Math.max(1, baseThreat);
        final var safeReference = Math.max(1, referenceThreat);
        final var targetPriorityWeight = safeReference * priorityFactor;
        final var weight = safeBase
            + clamp(score, 0.0, 1.0) * Math.max(0.0, targetPriorityWeight - safeBase);
        return Math.max(1, (int) Math.round(weight));
    }
}
