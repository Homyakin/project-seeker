package ru.homyakin.seeker.game.personage.models;

public enum PersonageSlot {
    MAIN_HAND(1),
    OFF_HAND(2),
    ;

    public final int id;

    PersonageSlot(int id) {
        this.id = id;
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
