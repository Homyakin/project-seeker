package ru.homyakin.seeker.game.contraband.entity;

public enum ContrabandStatus {
    FOUND(1),
    SOLD_TO_MARKET(2),
    WAITING_RECEIVER(3),
    OPENED_SUCCESS(4),
    OPENED_FAILURE(5),
    EXPIRED(6),
    ;

    private final int id;

    ContrabandStatus(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public boolean isFinal() {
        return this == OPENED_SUCCESS || this == OPENED_FAILURE || this == EXPIRED;
    }

    public static ContrabandStatus findById(int id) {
        for (ContrabandStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid ContrabandStatus ID: " + id);
    }
}
