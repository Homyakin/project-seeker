package ru.homyakin.seeker.game.random.item.entity.pool;

import ru.homyakin.seeker.game.personage.models.PersonageSlot;

public record ItemParamsWithoutRarity(
    PersonageSlot slot,
    int modifiersCount
) {
}
