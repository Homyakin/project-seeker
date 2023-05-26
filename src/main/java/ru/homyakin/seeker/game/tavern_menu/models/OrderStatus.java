package ru.homyakin.seeker.game.tavern_menu.models;

public enum OrderStatus {
    CREATED(1),
    EXPIRED(2),
    TECH_CANCEL(3),
    ACCEPTED(4),
    ;

    private final int id;

    OrderStatus(int id) {
        this.id = id;
    }

    public int id() {
        return id;
    }

    public static OrderStatus findById(int id) {
        for (OrderStatus status : values()) {
            if (status.id == id) {
                return status;
            }
        }
        throw new IllegalArgumentException("Invalid OrderStatus ID: " + id);
    }

    public boolean isFinal() {
        return this != CREATED;
    }
}
