package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.raid.RaidLocalization;

import java.util.List;

public record JoinToRaidResult(
    LaunchedEvent launchedEvent,
    Raid raid,
    List<Personage> personages
) {
    public String toMessage(Language language) {
        return raid.toStartMessage(language, launchedEvent.startDate(), launchedEvent.endDate())
            + "\n\n"
            + RaidLocalization.raidParticipants(language, personages);
    }
}
