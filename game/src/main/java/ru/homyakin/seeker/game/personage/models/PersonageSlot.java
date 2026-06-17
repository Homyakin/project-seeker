package ru.homyakin.seeker.game.personage.models;

import ru.homyakin.seeker.infrastructure.Icons;

public enum PersonageSlot {
    MAIN_HAND(1, Icons.MAIN_HAND),
    OFF_HAND(2, Icons.OFF_HAND),
    BODY(3, Icons.BODY),
    PANTS(4, Icons.PANTS),
    SHOES(5, Icons.SHOES),
    HELMET(6, Icons.HELMET),
    GLOVES(7, Icons.GLOVES),
    ;

    public final int id;
    public final String icon;

    PersonageSlot(int id, String icon) {
        this.id = id;
        this.icon = icon;
    }

    public static PersonageSlot findById(int id) {
        for (final var slot : values()) {
            if (slot.id == id) {
                return slot;
            }
        }
        throw new IllegalArgumentException("Invalid personage slot id: " + id);
    }
}
