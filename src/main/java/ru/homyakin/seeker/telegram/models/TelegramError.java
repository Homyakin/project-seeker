package ru.homyakin.seeker.telegram.models;

public sealed interface TelegramError {
    record InternalError(String message) implements TelegramError {}

    enum UserNotFound implements TelegramError { INSTANCE }
}
