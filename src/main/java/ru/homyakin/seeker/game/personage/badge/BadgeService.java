package ru.homyakin.seeker.game.personage.badge;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingBadge;

@Service
public class BadgeService {
    private final BadgeDao badgeDao;

    @Autowired
    public BadgeService(BadgeDao badgeDao) {
        this.badgeDao = badgeDao;
    }

    public void save(SavingBadge badge) {
        badgeDao.save(badge);
    }
}
