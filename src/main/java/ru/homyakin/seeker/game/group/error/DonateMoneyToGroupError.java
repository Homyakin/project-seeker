package ru.homyakin.seeker.game.group.error;

public sealed interface DonateMoneyToGroupError {
    enum NotEnoughMoney implements DonateMoneyToGroupError {
        INSTANCE
    }

    enum InvalidAmount implements DonateMoneyToGroupError {
        INSTANCE
    }
}
