package ru.homyakin.seeker.game.group.error;

import ru.homyakin.seeker.game.models.Money;

public sealed interface GroupRegistrationError {
    enum GroupAlreadyRegistered implements GroupRegistrationError {
        INSTANCE
    }

    enum PersonageInAnotherGroup implements GroupRegistrationError {
        INSTANCE
    }

    enum HiddenGroup implements GroupRegistrationError {
        INSTANCE
    }

    enum InvalidTag implements GroupRegistrationError {
        INSTANCE
    }

    enum TagAlreadyTaken implements GroupRegistrationError {
        INSTANCE
    }

    record NotEnoughMoney(Money required) implements GroupRegistrationError {}

    enum NotAdmin implements GroupRegistrationError {
        INSTANCE
    }
}
