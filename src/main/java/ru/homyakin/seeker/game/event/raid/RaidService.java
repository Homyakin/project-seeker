package ru.homyakin.seeker.game.event.raid;

import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.event.raid.models.Raid;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingRaid;

import java.util.Optional;

@Service
public class RaidService {
    private final RaidDao raidDao;

    public RaidService(RaidDao raidDao) {
        this.raidDao = raidDao;
    }

    public Optional<Raid> getRandomRaid() {
        return raidDao.getRandomRaid();
    }

    public Optional<Raid> getByEventId(int eventId) {
        return raidDao.getByEventId(eventId);
    }

    public void save(int eventId, SavingRaid raid) {
        raidDao.save(eventId, raid);
    }
}
