package ru.homyakin.seeker.game.outpost.entity;

import java.util.Arrays;
import java.util.Optional;

public enum Building {
    MONOLITH(1, 99),
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

    public static Optional<Building> fromId(int id) {
        return Arrays.stream(values()).filter(b -> b.id == id).findFirst();
    }
}
