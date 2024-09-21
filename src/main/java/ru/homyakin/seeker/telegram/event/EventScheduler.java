package ru.homyakin.seeker.telegram.event;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class EventScheduler {
    private final EventManager eventManager;

    public EventScheduler(EventManager eventManager) {
        this.eventManager = eventManager;
    }

    // Если шедулер запускается моментально, то приложение не успевает инициализировать локализацию
    // пока забагфикшено кроном, но это выглядит плохим решением
    @Scheduled(cron = "0 * * * * *")
    public void scheduledEventsLaunch() {
        eventManager.launchEventsInGroups();
    }

    @Scheduled(cron = "0 * * * * *")
    public void scheduledStopEvents() {
        eventManager.stopEvents();
    }
}
