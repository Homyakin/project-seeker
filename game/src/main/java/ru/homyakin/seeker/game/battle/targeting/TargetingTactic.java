package ru.homyakin.seeker.game.battle.targeting;

import java.util.List;

import ru.homyakin.seeker.game.battle.BattlePersonage;

/**
 * Targeting tactic: only adjusts local target weights; never picks the target itself
 * and never mutates global threat.
 * <p>
 * Weight uses reference threat among already-filtered candidates:
 * {@code weight = baseThreat + referenceThreat * bonusFactor * score}.
 */
public enum TargetingTactic {
    THREAT(0.0) {
        @Override
        double score(
            BattlePersonage attacker,
            BattlePersonage target,
            List<BattlePersonage> candidates
        ) {
            return 0.0;
        }

        @Override
        public int localWeight(
            BattlePersonage attacker,
            BattlePersonage target,
            List<BattlePersonage> candidates,
            int referenceThreat
        ) {
            return Math.max(1, target.totalThreat());
        }
    },
    EXECUTIONER(TargetingTacticCoefficients.EXECUTIONER_BONUS_FACTOR) {
        @Override
        double score(
            BattlePersonage attacker,
            BattlePersonage target,
            List<BattlePersonage> candidates
        ) {
            final var expectedDamage = attacker.expectedDamageAgainst(target);
            return TargetingMath.clamp(
                expectedDamage / Math.max(1, target.health()),
                0.0,
                1.0
            );
        }
    },
    WOUNDED_HUNTER(TargetingTacticCoefficients.WOUNDED_BONUS_FACTOR) {
        @Override
        double score(
            BattlePersonage attacker,
            BattlePersonage target,
            List<BattlePersonage> candidates
        ) {
            return 1.0 - target.percentHp() / 100.0;
        }
    },
    EXPLOIT_WEAKNESS(TargetingTacticCoefficients.EXPLOIT_BONUS_FACTOR) {
        @Override
        double score(
            BattlePersonage attacker,
            BattlePersonage target,
            List<BattlePersonage> candidates
        ) {
            if (candidates.size() <= 1) {
                return 0.0;
            }
            double minEv = Double.POSITIVE_INFINITY;
            double maxEv = Double.NEGATIVE_INFINITY;
            for (final var candidate : candidates) {
                final var ev = attacker.expectedDamageAgainst(candidate);
                minEv = Math.min(minEv, ev);
                maxEv = Math.max(maxEv, ev);
            }
            return TargetingMath.relativeAdvantageScore(
                attacker.expectedDamageAgainst(target),
                minEv,
                maxEv
            );
        }
    },
    RELIABLE_STRIKE(TargetingTacticCoefficients.RELIABLE_BONUS_FACTOR) {
        @Override
        double score(
            BattlePersonage attacker,
            BattlePersonage target,
            List<BattlePersonage> candidates
        ) {
            return 1.0 - target.dodgeChance() / 100.0;
        }
    },
    CHALLENGE_THE_AGILE(TargetingTacticCoefficients.CHALLENGE_BONUS_FACTOR) {
        @Override
        double score(
            BattlePersonage attacker,
            BattlePersonage target,
            List<BattlePersonage> candidates
        ) {
            return target.dodgeChance() / 100.0;
        }
    },
    INITIATIVE_INTERCEPTION(TargetingTacticCoefficients.INITIATIVE_BONUS_FACTOR) {
        @Override
        double score(
            BattlePersonage attacker,
            BattlePersonage target,
            List<BattlePersonage> candidates
        ) {
            return TargetingMath.clamp(
                1.0 - target.ticksUntilNextTurn()
                    / TargetingTacticCoefficients.INITIATIVE_INTERCEPTION_WINDOW,
                0.0,
                1.0
            );
        }
    },
    ;

    private final double bonusFactor;

    TargetingTactic(double bonusFactor) {
        this.bonusFactor = bonusFactor;
    }

    abstract double score(
        BattlePersonage attacker,
        BattlePersonage target,
        List<BattlePersonage> candidates
    );

    /**
     * Local targeting weight for an already range/position-filtered candidate.
     * Does not mutate target threat.
     */
    public int localWeight(
        BattlePersonage attacker,
        BattlePersonage target,
        List<BattlePersonage> candidates,
        int referenceThreat
    ) {
        return TargetingMath.targetingWeight(
            target.totalThreat(),
            score(attacker, target, candidates),
            referenceThreat,
            bonusFactor
        );
    }

    public static TargetingTactic fromString(String value) {
        return valueOf(value);
    }
}
