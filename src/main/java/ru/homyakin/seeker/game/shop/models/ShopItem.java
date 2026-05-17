package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.item.models.LegacyItem;
import ru.homyakin.seeker.game.models.Money;

public interface ShopItem {
    record Buy(Money price, ShopItemType type) implements ShopItem {
    }

    record Sell(Money price, LegacyItem item) implements ShopItem {
    }
}
