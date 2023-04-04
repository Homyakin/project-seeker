package ru.homyakin.seeker.utils.models.errors;

public sealed interface TelegramError {
    record InternalError(String message) implements TelegramError {}

    enum UserNotFound implements TelegramError { INSTANCE }
}
