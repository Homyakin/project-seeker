package ru.homyakin.seeker.game.outpost.entity;

import java.util.Arrays;
import java.util.Optional;

public enum Building {
    MONOLITH(1),
    ;

    private final int id;

    Building(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static Optional<Building> fromId(int id) {
        return Arrays.stream(values()).filter(b -> b.id == id).findFirst();
    }
}
