package ru.homyakin.seeker.game.item.errors;

import ru.homyakin.seeker.game.item.models.Item;

public sealed interface GenerateItemError {
    record NotEnoughSpace(Item item) implements GenerateItemError {}
}
