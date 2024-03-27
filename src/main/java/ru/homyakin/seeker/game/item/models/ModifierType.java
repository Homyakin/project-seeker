package ru.homyakin.seeker.game.item.models;

public enum ModifierType {
    PREFIX(1),
    SUFFIX(2),
    ;

    public final int id;

    ModifierType(int id) {
        this.id = id;
    }

    public static ModifierType findById(int id) {
        for (final var type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid modifier type id: " + id);
    }
}
