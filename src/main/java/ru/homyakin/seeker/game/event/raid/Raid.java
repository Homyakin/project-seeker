package ru.homyakin.seeker.game.event.raid;

public record Raid(
    int eventId,
    RaidTemplate template
) {
}
