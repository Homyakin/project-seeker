package ru.homyakin.seeker.game.battle.v4;

public enum Rarity {
    COMMON(0),
    UNCOMMON(1),
    RARE(3),
    EPIC(4),
    LEGENDARY(5);

    private final int skillPoints;

    Rarity(int skillPoints) {
        this.skillPoints = skillPoints;
    }

    public int skillPoints() {
        return skillPoints;
    }
}
