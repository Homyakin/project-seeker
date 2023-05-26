package ru.homyakin.seeker.game.tavern_menu.models;

import java.time.LocalDateTime;

public record MenuItemOrder(
    long id,
    int menuItemId,
    long orderingPersonageId,
    long acceptingPersonageId,
    LocalDateTime expireDateTime,
    OrderStatus status
) {
}
