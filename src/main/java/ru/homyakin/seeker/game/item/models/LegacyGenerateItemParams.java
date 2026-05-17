package ru.homyakin.seeker.game.item.models;

import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public record LegacyGenerateItemParams(
    LegacyItemRarity rarity,
    PersonageSlot slot,
    int modifierCount
) {
}
