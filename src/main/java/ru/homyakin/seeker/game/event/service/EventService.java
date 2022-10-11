package ru.homyakin.seeker.game.event.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.database.EventDao;
import ru.homyakin.seeker.game.event.models.Event;

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
