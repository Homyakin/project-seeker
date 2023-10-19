package ru.homyakin.seeker.game.tavern_menu.models;

import java.time.LocalDateTime;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public record MenuItemOrder(
    long id,
    int menuItemId,
    PersonageId orderingPersonageId,
    PersonageId acceptingPersonageId,
    LocalDateTime expireDateTime,
    OrderStatus status
) {
}
