package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public record FullItemParams(
    ItemRarity rarity,
    PersonageSlot slot,
    int modifiersCount
) {
}
