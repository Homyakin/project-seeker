package ru.homyakin.seeker.event.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.event.database.EventDao;
import ru.homyakin.seeker.event.models.Event;

@Service
public class EventService {
    private final EventDao eventDao;

    public EventService(
        EventDao eventDao
    ) {
        this.eventDao = eventDao;
    }

    public Event getRandomEvent() {
        return eventDao.getRandomEvent();
    }

    public Optional<Event> getEventById(Integer id) {
        return eventDao.getById(id);
    }
}
