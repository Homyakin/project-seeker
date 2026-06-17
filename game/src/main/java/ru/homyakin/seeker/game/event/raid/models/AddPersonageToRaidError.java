package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.personage.event.RaidParticipant;

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
            LaunchedRaidEvent launchedRaidEvent,
            List<RaidParticipant> participants
        ) implements RaidInFinalStatus {
        }
    }
}
