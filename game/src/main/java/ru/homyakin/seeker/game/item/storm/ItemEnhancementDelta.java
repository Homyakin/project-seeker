package ru.homyakin.seeker.game.item.storm;

import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;

/** Exact visible characteristic changes for the next storm-enhancement level. */
public record ItemEnhancementDelta(
    Optional<Integer> health,
    List<AttackDelta> attacks,
    Optional<DefenseDelta> defense
) {
    public ItemEnhancementDelta {
        attacks = List.copyOf(attacks);
    }

    public static ItemEnhancementDelta next(ItemObject object, int currentLevel) {
        if (object == null) {
            throw new IllegalArgumentException("Item object must be specified");
        }
        if (currentLevel < 0) {
            throw new IllegalArgumentException("Enhance level must be non-negative: " + currentLevel);
        }
        final int nextLevel = Math.incrementExact(currentLevel);
        final var current = new Item(object, Optional.empty(), ItemRarity.COMMON, currentLevel);
        final var next = new Item(object, Optional.empty(), ItemRarity.COMMON, nextLevel);
        final var currentAttacks = current.itemAttacks();
        final var nextAttacks = next.itemAttacks();
        if (currentAttacks.size() != nextAttacks.size()) {
            throw new IllegalStateException("Enhancement must preserve attack parts");
        }
        final var attacks = java.util.stream.IntStream.range(0, currentAttacks.size())
            .mapToObj(index -> {
                final var before = currentAttacks.get(index);
                final var after = nextAttacks.get(index);
                if (before.attackType() != after.attackType()
                    || before.minRange() != after.minRange()
                    || before.maxRange() != after.maxRange()) {
                    throw new IllegalStateException("Enhancement must preserve attack part identity");
                }
                return new AttackDelta(
                    before.attackType(),
                    before.minRange(),
                    before.maxRange(),
                    Math.subtractExact(after.attack(), before.attack())
                );
            })
            .toList();
        final Optional<DefenseDelta> defense = current.itemDefense().map(before -> {
            final var after = next.itemDefense().orElseThrow(
                () -> new IllegalStateException("Enhancement must preserve defense")
            );
            if (before.defenseType() != after.defenseType()) {
                throw new IllegalStateException("Enhancement must preserve defense type");
            }
            return new DefenseDelta(
                before.defenseType(),
                Math.subtractExact(after.defense(), before.defense())
            );
        });
        if (current.itemDefense().isEmpty() && next.itemDefense().isPresent()) {
            throw new IllegalStateException("Enhancement must not create defense");
        }
        return new ItemEnhancementDelta(
            object.health() > 0
                ? Optional.of(Math.subtractExact(next.health(), current.health()))
                : Optional.empty(),
            attacks,
            defense
        );
    }

    public boolean hasChanges() {
        return health.filter(it -> it > 0).isPresent()
            || attacks.stream().anyMatch(it -> it.attack() > 0)
            || defense.filter(it -> it.defense() > 0).isPresent();
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
}
