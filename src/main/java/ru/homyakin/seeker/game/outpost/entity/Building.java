package ru.homyakin.seeker.game.outpost.entity;

import java.util.Arrays;

public enum Building {
    MONOLITH(1, 5),
    ;

    private final int id;
    private final int maxLevel;

    Building(int id, int maxLevel) {
        this.id = id;
        this.maxLevel = maxLevel;
    }

    public int id() {
        return id;
    }

    public int maxLevel() {
        return maxLevel;
    }

    /**
     * Total materials the group must deliver to finish construction / upgrade to {@code targetLevel} (1-based).
     */
    public int materialsToReachLevel(int targetLevel) {
        return switch (this) {
            case MONOLITH -> 100 * targetLevel;
        };
    }

    public static Building fromId(int id) {
        return Arrays.stream(values()).filter(b -> b.id == id).findFirst().orElseThrow();
    }
}
