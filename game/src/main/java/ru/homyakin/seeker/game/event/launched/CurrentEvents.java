package ru.homyakin.seeker.game.event.launched;

import java.util.List;

public record CurrentEvents(
    List<CurrentEvent> events
) {
    public boolean hasBlockingEvent() {
        for (final var event: events) {
            if (event.type().isBlocking()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasId(long id) {
        for (final var event: events) {
            if (event.id() == id) {
                return true;
            }
        }
        return false;
    }
}
