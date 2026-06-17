package ru.homyakin.seeker.game.event.raid.models;

public record LaunchedRaidResult(
    Raid raid,
    LaunchedRaidEvent launchedRaidEvent,
    int energyCost
) {
}
