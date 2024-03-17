package ru.homyakin.seeker.game.item.models;

public enum ModifierType {
    PREFIX(1),
    SUFFIX(2),
    ;

    public final int id;

    ModifierType(int id) {
        this.id = id;
    }
}
