package ru.homyakin.seeker.game.item.errors;

import ru.homyakin.seeker.game.item.models.LegacyItem;

public sealed interface LegacyGenerateItemError {
    record NotEnoughSpace(LegacyItem item) implements LegacyGenerateItemError {}
}
