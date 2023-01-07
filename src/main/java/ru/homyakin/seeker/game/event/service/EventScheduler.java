package ru.homyakin.seeker.game.event.service;

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
        eventManager.launchEventsInGroups();
    }

    @Scheduled(fixedRateString = "PT1M")
    public void scheduledStopEvents() {
        eventManager.stopEvents();
    }
}
