package ru.homyakin.seeker.game.worker.error;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public sealed interface WorkerOfDayError {
    record NotEnoughUsers(int requiredUsers) implements WorkerOfDayError {
    }

    record AlreadyChosen(PersonageId personageId) implements WorkerOfDayError {
    }

    enum InternalError implements WorkerOfDayError { INSTANCE }

    enum NotRegisteredGroup implements WorkerOfDayError { INSTANCE }
}
