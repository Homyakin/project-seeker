package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.personage.event.RaidParticipant;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.raid.RaidLocalization;

import java.util.List;

public record JoinToRaidResult(
    LaunchedEvent launchedEvent,
    Raid raid,
    List<RaidParticipant> participants,
    boolean isExhausted
) {
    public String toMessage(Language language) {
        return raid.toStartMessage(language, launchedEvent.startDate(), launchedEvent.endDate())
            + "\n\n"
            + RaidLocalization.raidParticipants(language, participants);
    }
}
