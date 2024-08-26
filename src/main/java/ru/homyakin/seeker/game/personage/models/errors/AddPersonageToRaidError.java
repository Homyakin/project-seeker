package ru.homyakin.seeker.game.personage.models.errors;

import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.models.Raid;

public sealed interface AddPersonageToRaidError {
    enum RaidInProcess implements AddPersonageToRaidError { INSTANCE }

    enum RaidNotExist implements AddPersonageToRaidError { INSTANCE }

    enum PersonageInOtherEvent implements AddPersonageToRaidError { INSTANCE }

    enum PersonageInThisRaid implements AddPersonageToRaidError { INSTANCE }

    record EndedRaid(
        LaunchedEvent launchedEvent,
        Raid raid
    ) implements AddPersonageToRaidError {
    }

    record NotEnoughEnergy(int requiredEnergy) implements AddPersonageToRaidError { }
}
