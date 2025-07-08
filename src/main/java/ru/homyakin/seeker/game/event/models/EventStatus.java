package ru.homyakin.seeker.game.event.models;

public enum EventStatus {
    LAUNCHED(0),
    EXPIRED(1),
    FAILED(2),
    SUCCESS(3),
    CREATION_ERROR(4),
    CANCELED(5),
    ;

    private final int id;

    EventStatus(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static EventStatus findById(int id) {
        for (EventStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus ID: " + id);
    }

    public boolean isFinal() {
        return this != LAUNCHED;
    }
}
