package ru.homyakin.seeker.game.rumor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RumorService {
    private final RumorDao rumorDao;

    @Autowired
    public RumorService(RumorDao rumorDao) {
        this.rumorDao = rumorDao;
    }

    public Optional<Rumor> getRandomAvailableRumor() {
        return rumorDao.getRandomAvailableRumor();
    }

    public void save(Rumor rumor) {
        rumorDao.saveRumor(rumor);
    }
}
