package ru.homyakin.seeker.game.battle;

import java.util.Map;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;

/**
 * Numeric rules shared by combat and read-only characteristic calculations.
 */
public final class CombatRules {
    public static final double BASE_CRIT_MULTIPLIER = 1.2;
    public static final int INITIATIVE_THRESHOLD = 1000;
    public static final double DEFENSE_SCALE = 500;

    private static final Map<DefenseType, Map<AttackType, Double>> DAMAGE_MATRIX = Map.of(
        DefenseType.CLOTH, Map.of(
            AttackType.SLASH, 0.75,
            AttackType.BLUNT, 1.25,
            AttackType.PIERCE, 0.9,
            AttackType.MAGICAL, 1.1
        ),
        DefenseType.LEATHER, Map.of(
            AttackType.SLASH, 0.9,
            AttackType.BLUNT, 1.1,
            AttackType.PIERCE, 1.25,
            AttackType.MAGICAL, 0.75
        ),
        DefenseType.PLATE, Map.of(
            AttackType.SLASH, 1.25,
            AttackType.BLUNT, 0.75,
            AttackType.PIERCE, 1.1,
            AttackType.MAGICAL, 0.9
        ),
        DefenseType.ARCANE, Map.of(
            AttackType.SLASH, 1.1,
            AttackType.BLUNT, 0.9,
            AttackType.PIERCE, 0.75,
            AttackType.MAGICAL, 1.25
        )
    );

    private CombatRules() {
    }

    public static double defenseWeight(DefenseType defenseType, AttackType attackType) {
        if (defenseType == null || attackType == null) {
            throw new IllegalArgumentException("Attack and defense types must be specified");
        }
        return DAMAGE_MATRIX.get(defenseType).get(attackType);
    }

    public static double effectiveDefense(Map<DefenseType, Integer> defenses, AttackType attackType) {
        double result = 0;
        for (final var entry : defenses.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException("Defense must be non-negative: " + entry.getValue());
            }
            result += entry.getValue() * defenseWeight(entry.getKey(), attackType);
        }
        return result;
    }

    public static double damageTakenMultiplier(Map<DefenseType, Integer> defenses, AttackType attackType) {
        final var effectiveDefense = effectiveDefense(defenses, attackType);
        return 1 - effectiveDefense / (effectiveDefense + DEFENSE_SCALE);
    }

    public static double cappedTurnFrequency(int speed) {
        if (speed < 0) {
            throw new IllegalArgumentException("Speed must be non-negative: " + speed);
        }
        return (double) Math.min(speed, INITIATIVE_THRESHOLD) / INITIATIVE_THRESHOLD;
    }
}
