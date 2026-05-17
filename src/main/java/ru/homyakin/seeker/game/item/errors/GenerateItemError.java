package ru.homyakin.seeker.game.item.errors;

import ru.homyakin.seeker.game.item.models.PersonageItem;

public sealed interface GenerateItemError {
    record NotEnoughSpace(PersonageItem item) implements GenerateItemError {
    }
}
