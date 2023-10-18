package ru.homyakin.seeker.telegram.group.models;

import ru.homyakin.seeker.telegram.user.models.UserId;

public sealed interface SpinError {
    record NotEnoughUsers(int requiredUsers) implements SpinError {
    }

    record AlreadyChosen(UserId userId) implements SpinError {
    }

    enum InternalError implements SpinError { INSTANCE }
}
