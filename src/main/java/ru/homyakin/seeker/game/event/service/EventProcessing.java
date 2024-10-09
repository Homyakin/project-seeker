package ru.homyakin.seeker.game.event.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.models.EventResult;
import ru.homyakin.seeker.game.event.launched.LaunchedEvent;
import ru.homyakin.seeker.game.event.personal_quest.PersonalQuestService;
import ru.homyakin.seeker.game.event.raid.processing.RaidProcessing;

@Service
public class EventProcessing {
    private static final Logger logger = LoggerFactory.getLogger(EventProcessing.class);
    private final EventService eventService;
    private final RaidProcessing raidProcessing;
    private final PersonalQuestService personalQuestService;

    public EventProcessing(
        EventService eventService,
        RaidProcessing raidProcessing,
        PersonalQuestService personalQuestService
    ) {
        this.eventService = eventService;
        this.raidProcessing = raidProcessing;
        this.personalQuestService = personalQuestService;
    }

    public EventResult processEvent(LaunchedEvent launchedEvent) {
        if (launchedEvent.isInFinalStatus()) {
            logger.error("Processing launched event that is in final status " + launchedEvent.id());
        }
        final var event = eventService.getEventById(launchedEvent.eventId())
            .orElseThrow(() -> new IllegalStateException("Can't finish unknown event " + launchedEvent.eventId()));

        return switch (event.type()) {
            case RAID -> raidProcessing.process(launchedEvent);
            case PERSONAL_QUEST -> personalQuestService.stopQuest(launchedEvent);
        };
    }
}
