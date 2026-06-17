package ru.homyakin.seeker.telegram.group.taver_menu;

import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowOrderError;

public sealed interface ThrowOrderTgError {
    record Domain(ThrowOrderError error) implements ThrowOrderTgError {
    }

    enum UserNotFound implements ThrowOrderTgError {
        INSTANCE
    }
}
