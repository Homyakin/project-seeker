package ru.homyakin.seeker.game.event.raid;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.raid.models.Raid;

import java.util.Optional;

@Service
public class RaidService {
    private final RaidDao raidDao;

    public RaidService(RaidDao raidDao) {
        this.raidDao = raidDao;
    }

    public Optional<Raid> getByEventId(int eventId) {
        return raidDao.getByEventId(eventId);
    }
}
