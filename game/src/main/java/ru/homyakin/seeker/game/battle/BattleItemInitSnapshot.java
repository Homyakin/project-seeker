package ru.homyakin.seeker.game.battle;

import java.util.Optional;
import ru.homyakin.seeker.game.item.models.ItemRarity;

public record BattleItemInitSnapshot(
    Optional<String> code,
    ItemRarity rarity,
    Optional<String> modifier
) {
}
