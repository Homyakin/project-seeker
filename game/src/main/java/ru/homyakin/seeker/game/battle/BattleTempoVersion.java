package ru.homyakin.seeker.game.battle;

/** Versioned battle-wide damage pacing, independent of equipment and skill formulas. */
public enum BattleTempoVersion {
    BASELINE_V1(1, 1),
    ACCELERATED_V1(7, 4);

    private final int numerator;
    private final int denominator;

    BattleTempoVersion(int numerator, int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /** Applies the final multiplier once, after defense and variance, before the remaining-health cap. */
    public int scaleDamage(int damage) {
        if (damage < 0) {
            throw new IllegalArgumentException("Damage must be non-negative");
        }
        final long scaled = (Math.multiplyExact((long) damage, numerator) + denominator / 2L) / denominator;
        return Math.toIntExact(scaled);
    }

    public double damageMultiplier() {
        return (double) numerator / denominator;
    }
}
