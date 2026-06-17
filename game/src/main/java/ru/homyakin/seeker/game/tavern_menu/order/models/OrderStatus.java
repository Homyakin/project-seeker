package ru.homyakin.seeker.game.tavern_menu.order.models;

public enum OrderStatus {
    CREATED(1),
    EXPIRED(2),
    TECH_CANCEL(3),
    CONSUMED(4),
    CONSUMED_AND_EXPIRED(5),
    THROWN(6),
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
}
