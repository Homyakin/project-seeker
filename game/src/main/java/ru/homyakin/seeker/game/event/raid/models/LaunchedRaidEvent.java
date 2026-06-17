package ru.homyakin.seeker.game.event.raid.models;

import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.launched.RaidParams;
import ru.homyakin.seeker.game.event.models.EventStatus;

import java.time.Duration;
import java.time.LocalDateTime;

public record LaunchedRaidEvent(
    long id,
    int eventId,
    LocalDateTime startDate,
    LocalDateTime endDate,
    EventStatus status,
    RaidParams raidParams
) {
    public boolean isInFinalStatus() {
        return status.isFinal();
    }

    public Duration duration() {
        return Duration.between(startDate, endDate);
    }

    public static LaunchedRaidEvent fromLaunchedEvent(LaunchedEvent launchedEvent) {
        return new LaunchedRaidEvent(
            launchedEvent.id(),
            launchedEvent.eventId(),
            launchedEvent.startDate(),
            launchedEvent.endDate(),
            launchedEvent.status(),
            (RaidParams) launchedEvent.eventParams().orElseGet(() -> new RaidParams(10, 0))
        );
    }
}
