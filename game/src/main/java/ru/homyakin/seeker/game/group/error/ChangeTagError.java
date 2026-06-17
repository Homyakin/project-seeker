package ru.homyakin.seeker.game.group.error;

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

    enum NotAdmin implements ChangeTagError {
        INSTANCE
    }
}
