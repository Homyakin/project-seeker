package ru.homyakin.seeker.game.group.error;

public sealed interface GroupRegistrationError {
    enum GroupAlreadyRegistered implements GroupRegistrationError {
        INSTANCE
    }

    enum PersonageInAnotherGroup implements GroupRegistrationError {
        INSTANCE
    }

    enum PersonageNotGroupMember implements GroupRegistrationError {
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

    enum NotAdmin implements GroupRegistrationError {
        INSTANCE
    }

    record MonolithLevelTooLow(int requiredLevel) implements GroupRegistrationError {
    }
}
