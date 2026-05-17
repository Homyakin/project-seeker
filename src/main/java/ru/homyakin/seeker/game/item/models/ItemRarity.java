package ru.homyakin.seeker.game.item.models;

import java.util.Optional;

public enum ItemRarity {
    COMMON(0),
    UNCOMMON(1),
    RARE(2),
    EPIC(3),
    LEGENDARY(4);

    private final int skillPoints;

    ItemRarity(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public int skillPoints() {
        return skillPoints;
    }

    public String icon() {
        return LegacyItemRarity.valueOf(name()).icon;
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
