package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.item.models.LegacyItem;

import java.util.Optional;

public record AvailableAction(
    Optional<EnhanceAction> action,
    LegacyItem item
) {
}
