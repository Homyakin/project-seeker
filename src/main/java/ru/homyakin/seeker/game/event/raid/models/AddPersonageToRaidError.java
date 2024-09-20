package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.personage.models.Personage;

import java.util.List;

public sealed interface AddPersonageToRaidError {
    enum RaidInProcess implements AddPersonageToRaidError { INSTANCE }

    enum RaidNotExist implements AddPersonageToRaidError { INSTANCE }

    enum PersonageInOtherEvent implements AddPersonageToRaidError { INSTANCE }

    enum PersonageInThisRaid implements AddPersonageToRaidError { INSTANCE }

    sealed interface RaidInFinalStatus extends AddPersonageToRaidError {
        enum ExpiredRaid implements RaidInFinalStatus {
            INSTANCE
        }

        enum CreationErrorRaid implements RaidInFinalStatus {
            INSTANCE
        }

        record CompletedRaid(
            Raid raid,
            List<Personage> personages
        ) implements RaidInFinalStatus {
        }
    }

    record NotEnoughEnergy(int requiredEnergy) implements AddPersonageToRaidError { }
}
