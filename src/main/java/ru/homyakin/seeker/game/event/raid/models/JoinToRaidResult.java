package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.personage.event.RaidParticipant;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.raid.RaidLocalization;

import java.util.List;

public record JoinToRaidResult(
    LaunchedRaidEvent launchedRaidEvent,
    Raid raid,
    List<RaidParticipant> participants,
    boolean isExhausted,
    int raidEnergyCost
) {
    public String toMessage(Language language) {
        return raid.toStartMessage(
            language,
            launchedRaidEvent.startDate(),
            launchedRaidEvent.endDate(),
            launchedRaidEvent.raidParams().raidLevel()
        )
            + "\n\n"
            + RaidLocalization.raidParticipants(language, participants);
    }
}
