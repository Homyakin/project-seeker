package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.models.Money;

public record SoldItem(
    Item item,
    Money price
) {
}
