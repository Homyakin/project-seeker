package ru.homyakin.seeker.game.tavern_menu.order.models;

public record ExpiredOrder(
    long id,
    Status status
) {
    public enum Status {
        EXPIRED(OrderStatus.EXPIRED),
        CONSUMED_AND_EXPIRED(OrderStatus.CONSUMED_AND_EXPIRED),
        ;
        public final OrderStatus status;

        Status(OrderStatus status) {
            this.status = status;
        }
    }
}
