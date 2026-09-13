package ru.homyakin.seeker.game.item.storm;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;

/**
 * Pure version-one item progression. Derived states and transition deltas are never persisted.
 */
public final class ItemProgression {
    public static final int VERSION = 1;
    private static final int GROWTH_DIVISOR = 60;
    private static final int HALF_DIVISOR = GROWTH_DIVISOR / 2;

    private ItemProgression() {
    }

    public static int valueAtLevel(int base, int level) {
        if (base < 0) {
            throw new IllegalArgumentException("Base value must be non-negative: " + base);
        }
        if (level < 0) {
            throw new IllegalArgumentException("Enhance level must be non-negative: " + level);
        }
        if (base == 0 || level == 0) {
            return base;
        }
        final var scaledNumerator = Math.addExact(Math.multiplyExact((long) level, base), HALF_DIVISOR);
        final var increase = scaledNumerator / GROWTH_DIVISOR;
        return Math.toIntExact(Math.addExact((long) base, increase));
    }

    public static int transitionIncrease(int base, int currentLevel) {
        if (currentLevel == Integer.MAX_VALUE) {
            throw new ArithmeticException("No level exists after " + currentLevel);
        }
        return Math.subtractExact(
            valueAtLevel(base, currentLevel + 1),
            valueAtLevel(base, currentLevel)
        );
    }

    public static State state(ItemObject object, int level) {
        validateGrowthChannels(object);
        return new State(
            valueAtLevel(object.health(), level),
            object.attacks().stream()
                .map(attack -> new ItemAttack(
                    attack.attackType(),
                    attack.minRange(),
                    attack.maxRange(),
                    valueAtLevel(attack.attack(), level)
                ))
                .toList(),
            object.defense().map(defense -> new ItemDefense(
                defense.defenseType(),
                valueAtLevel(defense.defense(), level)
            ))
        );
    }

    public static Delta transition(ItemObject object, int currentLevel) {
        validateGrowthChannels(object);
        return new Delta(
            transitionIncrease(object.health(), currentLevel),
            object.attacks().stream()
                .map(attack -> new AttackDelta(
                    attack.attackType(),
                    attack.minRange(),
                    attack.maxRange(),
                    transitionIncrease(attack.attack(), currentLevel)
                ))
                .toList(),
            object.defense().map(defense -> new DefenseDelta(
                defense.defenseType(),
                transitionIncrease(defense.defense(), currentLevel)
            ))
        );
    }

    public static int maxSupportedLevel(ItemObject object) {
        validateGrowthChannels(object);
        var result = Integer.MAX_VALUE;
        if (object.health() > 0) {
            result = Math.min(result, maxSupportedLevel(object.health()));
        }
        for (final var attack : object.attacks()) {
            result = Math.min(result, maxSupportedLevel(attack.attack()));
        }
        if (object.defense().isPresent()) {
            result = Math.min(result, maxSupportedLevel(object.defense().get().defense()));
        }
        return result;
    }

    private static int maxSupportedLevel(int base) {
        final var availableIncrease = Integer.MAX_VALUE - (long) base;
        final var numerator = Math.subtractExact(
            Math.multiplyExact(GROWTH_DIVISOR, availableIncrease + 1),
            HALF_DIVISOR + 1L
        );
        return (int) Math.min(Integer.MAX_VALUE, numerator / base);
    }

    private static void validateGrowthChannels(ItemObject object) {
        if (object == null) {
            throw new IllegalArgumentException("Item object must be specified");
        }
        if (object.health() < 0) {
            throw new IllegalArgumentException("Health must be non-negative: " + object.health());
        }
        final Set<AttackKey> attackKeys = new HashSet<>();
        for (final var attack : object.attacks()) {
            final var key = new AttackKey(attack.attackType(), attack.minRange(), attack.maxRange());
            if (!attackKeys.add(key)) {
                throw new IllegalArgumentException("Duplicate attack part: " + key);
            }
        }
        if (object.defense().isPresent() && object.defense().get().defense() <= 0) {
            throw new IllegalArgumentException("Declared defense must be positive");
        }
        if (object.health() == 0 && object.attacks().isEmpty() && object.defense().isEmpty()) {
            throw new IllegalArgumentException("Item object has no growth channels");
        }
    }

    public record State(
        int health,
        List<ItemAttack> attacks,
        Optional<ItemDefense> defense
    ) {
        public State {
            attacks = List.copyOf(attacks);
        }
    }

    public record Delta(
        int health,
        List<AttackDelta> attacks,
        Optional<DefenseDelta> defense
    ) {
        public Delta {
            attacks = List.copyOf(attacks);
        }
    }

    public record AttackDelta(
        AttackType attackType,
        int minRange,
        int maxRange,
        int attack
    ) {
    }

    public record DefenseDelta(
        DefenseType defenseType,
        int defense
    ) {
    }

    private record AttackKey(
        AttackType attackType,
        int minRange,
        int maxRange
    ) {
    }
}
