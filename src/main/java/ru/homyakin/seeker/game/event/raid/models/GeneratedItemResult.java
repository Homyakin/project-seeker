package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.personage.models.Personage;

public sealed interface GeneratedItemResult {
    record Success(Personage personage, Item item) implements GeneratedItemResult {}

    record NotEnoughSpaceInBag(Personage personage, Item item) implements GeneratedItemResult {}
}
