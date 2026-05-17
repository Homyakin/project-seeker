package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.item.models.LegacyItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public record ItemParamsFull(
    LegacyItemRarity rarity,
    PersonageSlot slot,
    int modifiersCount
) {
}
