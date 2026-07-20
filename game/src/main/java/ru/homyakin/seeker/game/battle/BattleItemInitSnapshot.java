package ru.homyakin.seeker.game.battle;

import java.util.Optional;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.ItemRarity;

public record BattleItemInitSnapshot(
    String code,
    ItemRarity rarity,
    Optional<ActiveEnum> skill
) {
}
