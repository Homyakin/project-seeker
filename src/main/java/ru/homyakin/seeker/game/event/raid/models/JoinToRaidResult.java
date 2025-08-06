package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.personage.event.RaidParticipant;

import java.util.List;

public record JoinToRaidResult(
    LaunchedRaidEvent launchedRaidEvent,
    Raid raid,
    List<RaidParticipant> participants,
    boolean isExhausted,
    int raidEnergyCost
) {
}
