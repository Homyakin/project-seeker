package ru.homyakin.seeker.event;

import org.springframework.stereotype.Service;

@Service
public class EventService {
    private final GetEventDao getEventDao;

    public EventService(
        GetEventDao getEventDao
    ) {
        this.getEventDao = getEventDao;
    }

    public Event getRandomEvent() {
        return getEventDao.getRandomEvent();
    }
}
