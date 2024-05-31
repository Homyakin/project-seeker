package ru.homyakin.seeker.game.item.rarity;

import net.fellbaum.jemoji.EmojiManager;

public enum ItemRarity {
    COMMON(1, EmojiManager.getByAlias(":white_circle:").orElseThrow().getEmoji(), 1.0),
    UNCOMMON(2, EmojiManager.getByAlias(":green_circle:").orElseThrow().getEmoji(), 1.6),
    RARE(3, EmojiManager.getByAlias(":blue_circle:").orElseThrow().getEmoji(), 2.2),
    EPIC(4, EmojiManager.getByAlias(":purple_circle:").orElseThrow().getEmoji(), 2.8),
    LEGENDARY(5, EmojiManager.getByAlias(":orange_circle:").orElseThrow().getEmoji(), 3.4),
    ;

    public final int id;
    public final String icon;
    private final double multiplier;

    ItemRarity(int id, String icon, double multiplier) {
        this.id = id;
        this.icon = icon;
        this.multiplier = multiplier;
    }

    public double multiplier() {
        return multiplier;
    }

    public static ItemRarity findById(int id) {
        for (ItemRarity status : values()) {
            if (status.id == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ItemRarity id: " + id);
    }
}
