package ru.homyakin.seeker.game.item.models;

public enum Rarity {
    COMMON(0),
    UNCOMMON(1),
    RARE(2),
    EPIC(3),
    LEGENDARY(4);

    private final int skillPoints;

    Rarity(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public int skillPoints() {
        return skillPoints;
    }
}
