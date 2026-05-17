package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.models.Money;

public record SoldItem(
    PersonageItem item,
    Money price
) {
}
