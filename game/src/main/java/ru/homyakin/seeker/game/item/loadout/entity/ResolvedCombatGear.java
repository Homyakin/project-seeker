package ru.homyakin.seeker.game.item.loadout.entity;

import java.util.List;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.item.models.Item;

public record ResolvedCombatGear(
    List<Item> items,
    Position battlePosition
) {
}
