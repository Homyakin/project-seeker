package ru.homyakin.seeker.game.battle;

import java.util.Map;
import ru.homyakin.seeker.game.battle.skill.scaling.NonNegativeRational;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;

/**
 * Numeric rules shared by combat and read-only characteristic calculations.
 */
public final class CombatRules {
    public static final double BASE_CRIT_MULTIPLIER = 1.2;
    public static final int INITIATIVE_THRESHOLD = 1000;
    public static final double DEFENSE_SCALE = 500;
    private static final NonNegativeRational EXACT_DEFENSE_SCALE = NonNegativeRational.of(500);

    private static final Map<DefenseType, Map<AttackType, NonNegativeRational>> DAMAGE_MATRIX = Map.of(
        DefenseType.CLOTH, Map.of(
            AttackType.SLASH, NonNegativeRational.of(3, 4),
            AttackType.BLUNT, NonNegativeRational.of(5, 4),
            AttackType.PIERCE, NonNegativeRational.of(9, 10),
            AttackType.MAGICAL, NonNegativeRational.of(11, 10)
        ),
        DefenseType.LEATHER, Map.of(
            AttackType.SLASH, NonNegativeRational.of(9, 10),
            AttackType.BLUNT, NonNegativeRational.of(11, 10),
            AttackType.PIERCE, NonNegativeRational.of(5, 4),
            AttackType.MAGICAL, NonNegativeRational.of(3, 4)
        ),
        DefenseType.PLATE, Map.of(
            AttackType.SLASH, NonNegativeRational.of(5, 4),
            AttackType.BLUNT, NonNegativeRational.of(3, 4),
            AttackType.PIERCE, NonNegativeRational.of(11, 10),
            AttackType.MAGICAL, NonNegativeRational.of(9, 10)
        ),
        DefenseType.ARCANE, Map.of(
            AttackType.SLASH, NonNegativeRational.of(11, 10),
            AttackType.BLUNT, NonNegativeRational.of(9, 10),
            AttackType.PIERCE, NonNegativeRational.of(3, 4),
            AttackType.MAGICAL, NonNegativeRational.of(5, 4)
        )
    );

    private CombatRules() {
    }

    public static double defenseWeight(DefenseType defenseType, AttackType attackType) {
        return exactDefenseWeight(defenseType, attackType).doubleValue();
    }

    public static NonNegativeRational exactDefenseWeight(DefenseType defenseType, AttackType attackType) {
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

    public static NonNegativeRational exactEffectiveDefense(
        Map<DefenseType, Integer> defenses,
        AttackType attackType
    ) {
        var result = NonNegativeRational.ZERO;
        for (final var entry : defenses.entrySet()) {
            if (entry.getValue() < 0) {
                throw new IllegalArgumentException("Defense must be non-negative: " + entry.getValue());
            }
            result = result.add(exactDefenseWeight(entry.getKey(), attackType).multiply(entry.getValue()));
        }
        return result;
    }

    public static NonNegativeRational exactDamageTakenMultiplier(
        Map<DefenseType, Integer> defenses,
        AttackType attackType
    ) {
        final var denominator = EXACT_DEFENSE_SCALE.add(exactEffectiveDefense(defenses, attackType));
        return EXACT_DEFENSE_SCALE.divide(denominator);
    }

    public static double cappedTurnFrequency(int speed) {
        if (speed < 0) {
            throw new IllegalArgumentException("Speed must be non-negative: " + speed);
        }
        return (double) Math.min(speed, INITIATIVE_THRESHOLD) / INITIATIVE_THRESHOLD;
    }
}
