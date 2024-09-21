package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.event.models.LaunchedEvent;

public record LaunchedRaidResult(
    Raid raid,
    LaunchedEvent launchedEvent
) {
}
