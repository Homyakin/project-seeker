package ru.homyakin.seeker.game.event.launched;

import java.util.List;
import ru.homyakin.seeker.game.event.models.EventType;

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

    public boolean hasType(EventType type) {
        for (final var event : events) {
            if (event.type() == type) {
                return true;
            }
        }
        return false;
    }

    public boolean hasOtherOfType(EventType type, long excludeId) {
        for (final var event : events) {
            if (event.type() == type && event.id() != excludeId) {
                return true;
            }
        }
        return false;
    }
}
