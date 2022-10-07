package ru.homyakin.seeker.event.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.event.database.EventGetDao;
import ru.homyakin.seeker.event.models.Event;

@Service
public class EventService {
    private final EventGetDao eventGetDao;

    public EventService(
        EventGetDao eventGetDao
    ) {
        this.eventGetDao = eventGetDao;
    }

    public Event getRandomEvent() {
        return eventGetDao.getRandomEvent();
    }

    public Optional<Event> getEventById(Integer id) {
        return eventGetDao.getById(id);
    }
}
