package ru.homyakin.seeker.game.random.item.entity.pool;

import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public record FullItemParams(
    ItemRarity rarity,
    PersonageSlot slot,
    int modifiersCount
) {
}
