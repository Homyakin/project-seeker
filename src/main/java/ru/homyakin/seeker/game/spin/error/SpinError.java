package ru.homyakin.seeker.game.spin.error;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public sealed interface SpinError {
    record NotEnoughUsers(int requiredUsers) implements SpinError {
    }

    record AlreadyChosen(PersonageId personageId) implements SpinError {
    }

    enum InternalError implements SpinError { INSTANCE }
}
