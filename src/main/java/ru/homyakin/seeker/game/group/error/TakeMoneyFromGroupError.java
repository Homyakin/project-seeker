package ru.homyakin.seeker.game.group.error;

public sealed interface TakeMoneyFromGroupError {
    enum NotEnoughMoney implements TakeMoneyFromGroupError {
        INSTANCE
    }

    enum PersonageNotMember implements TakeMoneyFromGroupError {
        INSTANCE
    }

    enum GroupNotRegistered implements TakeMoneyFromGroupError {
        INSTANCE
    }
}
