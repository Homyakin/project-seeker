package ru.homyakin.seeker.game.tavern_menu.order.models;

import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.menu.models.Category;

public sealed interface ThrowOrderError {

    enum OrderLocked implements ThrowOrderError {
        INSTANCE
    }

    enum NoOrders implements ThrowOrderError {
        INSTANCE
    }

    record OnlyCreatedOrders(
        Category category
    ) implements ThrowOrderError {
    }

    record NotEnoughMoney(
        Money cost
    ) implements ThrowOrderError {
    }
}
