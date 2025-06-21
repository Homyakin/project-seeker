package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public record ItemParamsFull(
    ItemRarity rarity,
    PersonageSlot slot,
    int modifiersCount
) {
}
