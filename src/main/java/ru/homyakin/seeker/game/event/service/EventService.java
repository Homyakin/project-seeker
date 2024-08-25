package ru.homyakin.seeker.game.event.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.event.database.EventDao;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.raid.RaidService;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingRaid;

@Service
public class EventService {
    private final EventDao eventDao;
    private final RaidService raidService;

    public EventService(
        EventDao eventDao,
        RaidService raidService
    ) {
        this.eventDao = eventDao;
        this.raidService = raidService;
    }

    @Transactional
    public void saveRaid(SavingRaid raid) {
        final var id = eventDao.save(raid);
        raidService.save(id, raid);
    }

    public Optional<Event> getEventById(Integer id) {
        return eventDao.getById(id);
    }
}
