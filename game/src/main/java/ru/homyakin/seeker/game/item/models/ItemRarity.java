package ru.homyakin.seeker.game.item.models;

import java.util.Optional;
import net.fellbaum.jemoji.EmojiManager;

public enum ItemRarity {
    COMMON(0, EmojiManager.getByAlias(":white_circle:").orElseThrow().getFirst().getEmoji()),
    UNCOMMON(1, EmojiManager.getByAlias(":green_circle:").orElseThrow().getFirst().getEmoji()),
    RARE(2, EmojiManager.getByAlias(":blue_circle:").orElseThrow().getFirst().getEmoji()),
    EPIC(3, EmojiManager.getByAlias(":purple_circle:").orElseThrow().getFirst().getEmoji()),
    LEGENDARY(4, EmojiManager.getByAlias(":orange_circle:").orElseThrow().getFirst().getEmoji());

    private final int skillPoints;
    private final String icon;

    ItemRarity(int skillPoints, String icon) {
        this.skillPoints = skillPoints;
        this.icon = icon;
    }

    public int skillPoints() {
        return skillPoints;
    }

    public String icon() {
        return icon;
    }

    public Optional<ItemRarity> next() {
        return switch (this) {
            case COMMON -> Optional.of(UNCOMMON);
            case UNCOMMON -> Optional.of(RARE);
            case RARE -> Optional.of(EPIC);
            case EPIC -> Optional.of(LEGENDARY);
            case LEGENDARY -> Optional.empty();
        };
    }
}
