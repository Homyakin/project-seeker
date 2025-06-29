package ru.homyakin.seeker.game.badge.action;

import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.badge.entity.BadgeStorage;
import ru.homyakin.seeker.game.badge.infra.postgres.BadgeDao;
import ru.homyakin.seeker.game.badge.entity.Badge;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingBadge;

@Service
public class BadgeService {
    private final BadgeStorage storage;

    @Autowired
    public BadgeService(BadgeDao badgeDao) {
        this.storage = badgeDao;
    }

    public void save(SavingBadge badge) {
        storage.save(badge);
    }

    public Optional<Badge> getByCode(String code) {
        return storage.getByCode(code);
    }
}
