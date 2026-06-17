package ru.homyakin.seeker.game.personage.event;

public sealed interface CancelError {
    enum NotFound implements CancelError { INSTANCE }

    enum AlreadyFinished implements CancelError { INSTANCE }

    enum Locked implements CancelError { INSTANCE }
}
