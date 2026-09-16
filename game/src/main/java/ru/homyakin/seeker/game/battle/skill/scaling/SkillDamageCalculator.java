package ru.homyakin.seeker.game.battle.skill.scaling;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.IntBinaryOperator;
import ru.homyakin.seeker.game.battle.CombatRules;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;

/**
 * Exact arithmetic for one direct or periodic skill-damage application.
 */
public final class SkillDamageCalculator {
    private SkillDamageCalculator() {
    }

    /**
     * @param inclusiveSpreadRoll receives inclusive minimum and maximum deviation
     */
    public static Result calculate(
        Map<AttackType, NonNegativeRational> rawDamage,
        Map<DefenseType, Integer> defenses,
        IntBinaryOperator inclusiveSpreadRoll
    ) {
        Objects.requireNonNull(rawDamage, "rawDamage");
        Objects.requireNonNull(defenses, "defenses");
        Objects.requireNonNull(inclusiveSpreadRoll, "inclusiveSpreadRoll");

        final var normalizedRawDamage = normalize(rawDamage);
        var exactAfterDefense = NonNegativeRational.ZERO;
        for (final var entry : normalizedRawDamage.entrySet()) {
            exactAfterDefense = exactAfterDefense.add(entry.getValue().multiply(
                CombatRules.exactDamageTakenMultiplier(defenses, entry.getKey())
            ));
        }
        if (normalizedRawDamage.isEmpty()) {
            return new Result(Map.of(), NonNegativeRational.ZERO, 0, 0, 0, 0);
        }

        var damageAfterDefense = exactAfterDefense.floor();
        if (damageAfterDefense.signum() == 0) {
            damageAfterDefense = java.math.BigInteger.ONE;
        }
        final int protectedDamage = damageAfterDefense.intValueExact();
        final int spreadBound = protectedDamage / 10;
        final int deviation;
        if (spreadBound == 0) {
            deviation = 0;
        } else {
            deviation = inclusiveSpreadRoll.applyAsInt(-spreadBound, spreadBound);
            if (deviation < -spreadBound || deviation > spreadBound) {
                throw new IllegalArgumentException(
                    "Spread roll %d is outside [%d, %d]".formatted(deviation, -spreadBound, spreadBound)
                );
            }
        }
        final int finalDamage = Math.max(1, Math.addExact(protectedDamage, deviation));
        return new Result(
            normalizedRawDamage,
            exactAfterDefense,
            protectedDamage,
            spreadBound,
            deviation,
            finalDamage
        );
    }

    private static Map<AttackType, NonNegativeRational> normalize(
        Map<AttackType, NonNegativeRational> rawDamage
    ) {
        final var result = new EnumMap<AttackType, NonNegativeRational>(AttackType.class);
        for (final var entry : rawDamage.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "attack type");
            Objects.requireNonNull(entry.getValue(), "raw damage component");
            if (!entry.getValue().isZero()) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return Map.copyOf(result);
    }

    public record Result(
        Map<AttackType, NonNegativeRational> rawDamage,
        NonNegativeRational exactDamageAfterDefense,
        int damageAfterDefense,
        int spreadBound,
        int spreadDeviation,
        int damage
    ) {
        public Result {
            rawDamage = Map.copyOf(rawDamage);
        }
    }
}
