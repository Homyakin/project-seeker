package ru.homyakin.seeker.game.battle.targeting;

/**
 * Tunable balance constants for targeting tactics.
 * Tactical bonus added on top of base threat:
 * {@code weight = baseThreat + referenceThreat * BONUS_FACTOR * score}.
 */
public final class TargetingTacticCoefficients {
    public static final double EXECUTIONER_BONUS_FACTOR = 2.0;
    public static final double WOUNDED_BONUS_FACTOR = 1.5;
    public static final double EXPLOIT_BONUS_FACTOR = 1.75;
    public static final double RELIABLE_BONUS_FACTOR = 1.5;
    public static final double CHALLENGE_BONUS_FACTOR = 1.5;
    public static final double INITIATIVE_BONUS_FACTOR = 2.0;

    /**
     * Absolute ticks window for {@link TargetingTactic#INITIATIVE_INTERCEPTION}.
     * Score falls to 0 when {@code ticksUntilNextTurn >= WINDOW}.
     */
    public static final double INITIATIVE_INTERCEPTION_WINDOW = 8.0;

    private TargetingTacticCoefficients() {
    }
}
