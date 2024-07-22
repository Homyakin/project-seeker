package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.models.Money;

public interface ShopItem {
    record Buy(Money price, ShopItemType type) implements ShopItem {
    }

    record Sell(Money price, Item item) implements ShopItem {
    }
}
