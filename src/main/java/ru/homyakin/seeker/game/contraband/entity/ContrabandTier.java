package ru.homyakin.seeker.game.contraband.entity;

public enum ContrabandTier {
    COMMON(1),
    RARE(2),
    EPIC(3),
    ;

    private final int id;

    ContrabandTier(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static ContrabandTier findById(int id) {
        for (ContrabandTier tier : values()) {
            if (tier.id == id) {
                return tier;
            }
        }
        throw new IllegalArgumentException("Invalid ContrabandTier ID: " + id);
    }
}
