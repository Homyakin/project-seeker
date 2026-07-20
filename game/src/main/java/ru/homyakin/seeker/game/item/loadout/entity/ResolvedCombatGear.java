package ru.homyakin.seeker.game.item.loadout.entity;

import java.util.List;
import java.util.Optional;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.item.models.Item;

public record ResolvedCombatGear(
    List<Item> items,
    Optional<Position> battlePosition
) {
    public static ResolvedCombatGear fromEquipped(List<Item> items) {
        return new ResolvedCombatGear(items, Optional.empty());
    }

    public static ResolvedCombatGear fromLoadout(List<Item> items, Position battlePosition) {
        return new ResolvedCombatGear(items, Optional.of(battlePosition));
    }

    public Position positionOr(Position fallback) {
        return battlePosition.orElse(fallback);
    }
}
