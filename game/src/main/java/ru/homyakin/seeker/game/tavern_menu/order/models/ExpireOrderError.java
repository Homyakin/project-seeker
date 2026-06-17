package ru.homyakin.seeker.game.tavern_menu.order.models;

public sealed interface ExpireOrderError {
    enum OrderLocked implements ExpireOrderError {
        INSTANCE
    }

    enum InvalidStatus implements ExpireOrderError {
        INSTANCE
    }
}
