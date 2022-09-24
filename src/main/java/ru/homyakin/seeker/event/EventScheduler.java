package ru.homyakin.seeker.event;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EventScheduler {
    private final EventManager eventManager;

    public EventScheduler(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    @Scheduled(fixedRateString = "PT1M")
    public void scheduledEventsLaunch() {
        eventManager.launchEventsInChats();
    }

    @Scheduled(fixedRateString = "PT1M")
    public void scheduledStopEvents() {
        eventManager.stopEvents();
    }
}
