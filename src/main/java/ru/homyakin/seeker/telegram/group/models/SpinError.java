package ru.homyakin.seeker.telegram.group.models;

public interface SpinError {
    record NotEnoughUsers(int requiredUsers) implements SpinError {
    }

    record AlreadyChosen(long userId) implements SpinError {
    }
}
