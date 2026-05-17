package ru.homyakin.seeker.game.item.modifier.models;

public enum LegacyModifierType {
    PREFIX(1),
    SUFFIX(2),
    ;

    public final int id;

    LegacyModifierType(int id) {
        this.id = id;
    }

    public static LegacyModifierType findById(int id) {
        for (final var type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Invalid modifier type id: " + id);
    }
}
