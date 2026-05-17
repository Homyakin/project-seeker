package ru.homyakin.seeker.game.shop.models;

import java.util.Optional;
import ru.homyakin.seeker.game.item.models.PersonageItem;

public record AvailableAction(
    Optional<EnhanceAction> action,
    PersonageItem item
) {
}
