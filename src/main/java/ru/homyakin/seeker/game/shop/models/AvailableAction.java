package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.item.models.Item;

import java.util.Optional;

public record AvailableAction(
    Optional<EnhanceAction> action,
    Item item
) {
}
