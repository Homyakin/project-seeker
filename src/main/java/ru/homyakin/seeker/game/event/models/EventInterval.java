package ru.homyakin.seeker.game.event.models;

public record EventInterval(
    int startHour,
    int endHour,
    boolean isEnabled
) {
    public EventInterval toggle() {
        return new EventInterval(startHour, endHour, !isEnabled);
    }
}
