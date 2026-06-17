package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.event.launched.EventPersonageParams;

public record RaidPersonageParams(
    boolean isExhausted
) implements EventPersonageParams {
}
