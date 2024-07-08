package ru.homyakin.seeker.game.event.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.LaunchedEvent;
import ru.homyakin.seeker.game.event.raid.RaidProcessing;
import ru.homyakin.seeker.game.event.raid.models.RaidResult;
import ru.homyakin.seeker.game.personage.PersonageService;

@Service
public class EventProcessing {
    private final EventService eventService;
    private final PersonageService personageService;
    private final RaidProcessing raidProcessing;

    public EventProcessing(EventService eventService, PersonageService personageService, RaidProcessing raidProcessing) {
        this.eventService = eventService;
        this.personageService = personageService;
        this.raidProcessing = raidProcessing;
    }

    public Optional<RaidResult> processEvent(LaunchedEvent launchedEvent) {
        final var event = eventService.getEventById(launchedEvent.eventId())
            .orElseThrow(() -> new IllegalStateException("Can't finish unknown event " + launchedEvent.eventId()));

        return switch (event.type()) {
            case RAID -> raidProcessing.process(event, launchedEvent);
        };
    }
}
