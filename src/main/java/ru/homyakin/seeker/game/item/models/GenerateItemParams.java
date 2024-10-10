package ru.homyakin.seeker.game.item.models;

import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public record GenerateItemParams(
    ItemRarity rarity,
    PersonageSlot slot,
    int modifierCount
) {
}
