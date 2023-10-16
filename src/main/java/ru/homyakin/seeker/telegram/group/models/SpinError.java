package ru.homyakin.seeker.telegram.group.models;

public sealed interface SpinError {
    record NotEnoughUsers(int requiredUsers) implements SpinError {
    }

    record AlreadyChosen(long userId) implements SpinError {
    }

    enum InternalError implements SpinError { INSTANCE }
}
