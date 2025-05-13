package ru.homyakin.seeker.game.group.error;

import ru.homyakin.seeker.game.models.Money;

public sealed interface ChangeTagError {

    enum PersonageNotInGroup implements ChangeTagError {
        INSTANCE
    }

    enum GroupNotRegistered implements ChangeTagError {
        INSTANCE
    }

    enum InvalidTag implements ChangeTagError {
        INSTANCE
    }

    enum TagAlreadyTaken implements ChangeTagError {
        INSTANCE
    }

    record NotEnoughMoney(Money required) implements ChangeTagError {}

    enum NotAdmin implements ChangeTagError {
        INSTANCE
    }
}
