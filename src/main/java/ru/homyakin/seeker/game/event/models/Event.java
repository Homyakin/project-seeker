package ru.homyakin.seeker.game.event.models;

public record Event(
    int id,
    EventType type,
    String code
) {
}
