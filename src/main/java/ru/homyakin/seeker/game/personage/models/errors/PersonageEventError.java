package ru.homyakin.seeker.game.personage.models.errors;

import ru.homyakin.seeker.game.event.raid.models.Raid;

public sealed interface PersonageEventError {
    enum EventInProcess implements PersonageEventError { INSTANCE }

    enum EventNotExist implements PersonageEventError { INSTANCE }

    enum PersonageInOtherEvent implements PersonageEventError { INSTANCE }

    enum PersonageInThisEvent implements PersonageEventError { INSTANCE }

    record ExpiredEvent(
        Raid raid
    ) implements PersonageEventError {
    }

    record NotEnoughEnergy(int requiredEnergy) implements PersonageEventError { }
}
