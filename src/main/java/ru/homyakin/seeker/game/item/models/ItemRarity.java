package ru.homyakin.seeker.game.item.models;

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
}
