package ru.homyakin.seeker.game.personage.power;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import ru.homyakin.seeker.game.battle.CombatRules;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;

/**
 * Read-only display rating. Game mechanics must never consume this result.
 */
public final class ReferencePowerCalculator {
    private ReferencePowerCalculator() {
    }

    public static ReferencePower calculate(List<Item> items, double displayScale) {
        if (items == null || items.stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Items must be specified");
        }
        if (!Double.isFinite(displayScale) || displayScale <= 0) {
            throw new IllegalArgumentException("Display scale must be finite and positive: " + displayScale);
        }

        var health = 0;
        var speed = 0;
        var critChance = 0;
        var dodgeChance = 0;
        var critMultiplier = CombatRules.BASE_CRIT_MULTIPLIER;
        final Map<DefenseType, Integer> defenses = new EnumMap<>(DefenseType.class);
        final Map<Integer, Integer> attackDeltas = new TreeMap<>();

        for (final var item : items) {
            health = Math.addExact(health, nonNegative(item.health(), "health"));
            speed = Math.addExact(speed, nonNegative(item.speed(), "speed"));
            critChance = Math.addExact(critChance, nonNegative(item.critChance(), "crit chance"));
            dodgeChance = Math.addExact(dodgeChance, nonNegative(item.dodgeChance(), "dodge chance"));
            if (!Double.isFinite(item.critMultiplier()) || item.critMultiplier() < 0) {
                throw new IllegalArgumentException("Critical multiplier bonus must be finite and non-negative");
            }
            critMultiplier += item.critMultiplier();
            item.itemDefense().ifPresent(defense -> defenses.merge(
                defense.defenseType(),
                nonNegative(defense.defense(), "defense"),
                Math::addExact
            ));
            for (final var attack : item.itemAttacks()) {
                final var value = nonNegative(attack.attack(), "attack");
                attackDeltas.merge(attack.minRange(), value, Math::addExact);
                if (attack.maxRange() < Integer.MAX_VALUE) {
                    attackDeltas.merge(attack.maxRange() + 1, -value, Math::addExact);
                }
            }
        }

        if (critChance > 100) {
            throw new IllegalArgumentException("Critical chance must be in 0..100: " + critChance);
        }
        if (dodgeChance >= 100) {
            throw new IllegalArgumentException("Dodge chance must be in 0..99: " + dodgeChance);
        }
        if (!Double.isFinite(critMultiplier) || critMultiplier < 1) {
            throw new IllegalArgumentException("Critical multiplier must be finite and at least one");
        }

        final var workingAttack = workingAttack(attackDeltas);
        final var averageDamageTaken = java.util.Arrays.stream(AttackType.values())
            .mapToDouble(type -> CombatRules.damageTakenMultiplier(defenses, type))
            .average()
            .orElseThrow();
        final var dodgeMultiplier = 1 - dodgeChance / 100.0;
        final var survivability = health == 0 ? 0 : health / (dodgeMultiplier * averageDamageTaken);
        final var critFactor = 1 + critChance / 100.0 * (critMultiplier - 1);
        final var normalDamagePerTime = CombatRules.cappedTurnFrequency(speed) * critFactor * workingAttack;
        final var unscaled = survivability * normalDamagePerTime;
        final var displayed = unscaled * displayScale;
        if (!Double.isFinite(survivability) || !Double.isFinite(normalDamagePerTime) || !Double.isFinite(displayed)) {
            throw new IllegalArgumentException("Reference power result must be finite");
        }
        return new ReferencePower(survivability, normalDamagePerTime, unscaled, displayed);
    }

    private static int workingAttack(Map<Integer, Integer> deltas) {
        var current = 0;
        var maximum = 0;
        for (final var delta : deltas.values()) {
            current = Math.addExact(current, delta);
            maximum = Math.max(maximum, current);
        }
        return maximum;
    }

    private static int nonNegative(int value, String field) {
        if (value < 0) {
            throw new IllegalArgumentException(field + " must be non-negative: " + value);
        }
        return value;
    }
}
