package ru.homyakin.seeker.game.event.models;

import jakarta.validation.constraints.NotNull;

public record Event(
    int id,
    @NotNull
    EventType type
) {
}
