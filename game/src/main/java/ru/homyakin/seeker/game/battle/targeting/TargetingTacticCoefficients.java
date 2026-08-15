package ru.homyakin.seeker.game.battle.targeting;

/**
 * Tunable balance constants for targeting tactics.
 * Ideal match (score=1) aims for {@code maxCandidateThreat * PRIORITY_FACTOR} weight,
 * so a strong match outprioritizes the current top-threat target among candidates.
 */
public final class TargetingTacticCoefficients {
    public static final double EXECUTIONER_PRIORITY_FACTOR = 2.0;
    public static final double WOUNDED_PRIORITY_FACTOR = 1.5;
    public static final double EXPLOIT_PRIORITY_FACTOR = 1.75;
    public static final double RELIABLE_PRIORITY_FACTOR = 1.5;
    public static final double CHALLENGE_PRIORITY_FACTOR = 1.5;
    public static final double INITIATIVE_PRIORITY_FACTOR = 2.0;

    /**
     * Absolute ticks window for {@link TargetingTactic#INITIATIVE_INTERCEPTION}.
     * Score falls to 0 when {@code ticksUntilNextTurn >= WINDOW}.
     */
    public static final double INITIATIVE_INTERCEPTION_WINDOW = 8.0;

    /**
     * Relative EV spread below which {@link TargetingTactic#EXPLOIT_WEAKNESS} applies no bonus.
     * {@code (maxEv - minEv) / maxEv}.
     */
    public static final double EXPLOIT_MIN_RELATIVE_GAP = 0.05;

    private TargetingTacticCoefficients() {
    }
}
