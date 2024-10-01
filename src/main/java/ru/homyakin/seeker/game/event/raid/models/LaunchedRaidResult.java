package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.event.launched.LaunchedEvent;

public record LaunchedRaidResult(
    Raid raid,
    LaunchedEvent launchedEvent
) {
}
