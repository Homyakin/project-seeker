package ru.homyakin.seeker.game.event.pve;

import java.util.ArrayList;
import java.util.List;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.CombatRules;
import ru.homyakin.seeker.game.item.models.AttackType;

/**
 * Neutral technical assessment of prepared combat copies, not displayed equipment power.
 * Skills, dodge, targeting and the opportunity to use the best range profile are deliberately
 * not priced here; actual battles must validate the resulting enemy calibration.
 */
public final class PveTeamAssessment {
    public static final String VERSION = "NEUTRAL_TEAM_V1";

    private PveTeamAssessment() {
    }

    /** The product of the team's total effective health and total expected normal damage rate. */
    public static double assess(List<BattlePersonage> personages) {
        if (personages == null || personages.isEmpty()) {
            throw new IllegalArgumentException("A prepared PvE team must not be null or empty");
        }
        final var effectiveHealth = new ArrayList<Double>(personages.size());
        final var damageRates = new ArrayList<Double>(personages.size());
        for (final var personage : personages) {
            if (personage == null) {
                throw new IllegalArgumentException("A prepared PvE participant must not be null");
            }
            requireFiniteNonNegative(personage.maxHealth(), "Maximum health");
            requireFiniteNonNegative(personage.initiative(), "Speed");
            requireFiniteNonNegative(personage.critMultiplier(), "Critical multiplier");
            final var defenses = personage.defenses();
            if (defenses == null) {
                throw new IllegalArgumentException("Prepared defenses must not be null");
            }
            for (final var entry : defenses.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException("Prepared defenses must not contain null entries");
                }
                requireFiniteNonNegative(entry.getValue(), "Defense");
            }
            final var reductions = personage.defenseReductions();
            if (reductions == null || reductions.size() != AttackType.values().length) {
                throw new IllegalArgumentException("Prepared damage taken multipliers must cover all attack types");
            }
            var reductionSum = 0.0;
            for (final var type : AttackType.values()) {
                final var reduction = reductions.get(type);
                if (reduction == null || !Double.isFinite(reduction) || reduction <= 0) {
                    throw new IllegalArgumentException("Damage taken multiplier must be positive and finite: " + type);
                }
                reductionSum += reduction;
                requireFiniteNonNegative(reductionSum, "Damage taken multiplier sum");
            }
            final var health = personage.maxHealth() / (reductionSum / AttackType.values().length);
            requireFiniteNonNegative(health, "Effective health");
            effectiveHealth.add(health);

            final var critProbability = Math.clamp(personage.critChance() / 100.0, 0.0, 1.0);
            final var critFactor = 1 + critProbability * (personage.critMultiplier() - 1);
            requireFiniteNonNegative(critFactor, "Critical expectation");
            final var attack = maximumAttack(personage);
            final var frequency = CombatRules.cappedTurnFrequency(personage.initiative());
            final var damageRate = attack == 0 || critFactor == 0 || frequency == 0
                ? 0 : attack * critFactor * frequency;
            requireFiniteNonNegative(damageRate, "Expected normal damage rate");
            damageRates.add(damageRate);
        }
        final var assessment = stableSum(effectiveHealth) * stableSum(damageRates);
        requireFiniteNonNegative(assessment, "Team assessment");
        return assessment;
    }

    private static double maximumAttack(BattlePersonage personage) {
        final var profiles = personage.attacksByRange();
        if (profiles == null) {
            throw new IllegalArgumentException("Prepared attack profiles must not be null");
        }
        var maximum = 0.0;
        for (final var profile : profiles) {
            if (profile == null || profile.range() < 1 || profile.attack() == null) {
                throw new IllegalArgumentException("Prepared attack profiles must have a positive range and an attack map");
            }
            for (final var entry : profile.attack().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException("Prepared attacks must not contain null entries");
                }
                requireFiniteNonNegative(entry.getValue(), "Attack");
            }
            var atRange = 0.0;
            for (final var type : AttackType.values()) {
                atRange += profile.attack().getOrDefault(type, 0);
            }
            requireFiniteNonNegative(atRange, "Attack at one range");
            maximum = Math.max(maximum, atRange);
        }
        return maximum;
    }

    private static double stableSum(List<Double> contributions) {
        // Numeric ordering preserves the formula while making participant permutations bit-stable.
        contributions.sort(Double::compare);
        var total = 0.0;
        for (final var contribution : contributions) {
            total += contribution;
            requireFiniteNonNegative(total, "Team contribution sum");
        }
        return total;
    }

    private static void requireFiniteNonNegative(double value, String label) {
        if (!Double.isFinite(value) || value < 0) {
            throw new IllegalArgumentException(label + " must be finite and non-negative: " + value);
        }
    }
}
