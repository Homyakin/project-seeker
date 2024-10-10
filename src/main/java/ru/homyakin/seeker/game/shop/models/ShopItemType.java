package ru.homyakin.seeker.game.shop.models;

import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.infrastructure.Icons;

import java.util.Optional;

public enum ShopItemType {
    COMMON(0, "common", ItemRarity.COMMON.icon),
    UNCOMMON(1, "uncommon", ItemRarity.UNCOMMON.icon),
    RARE(2, "rare", ItemRarity.RARE.icon),
    EPIC(3, "epic", ItemRarity.EPIC.icon),
    LEGENDARY(4, "legendary", ItemRarity.LEGENDARY.icon),
    RANDOM(5, "random", Icons.RANDOM),
    ;

    public final int priority;
    public final String telegramCode;
    public final String icon;

    ShopItemType(int priority, String telegramCode, String icon) {
        this.priority = priority;
        this.telegramCode = telegramCode;
        this.icon = icon;
    }

    public static Optional<ShopItemType> findByCode(String code) {
        for (final var type : ShopItemType.values()) {
            if (type.telegramCode.equals(code)) {
                return Optional.of(type);
            }
        }
        return Optional.empty();
    }
}
