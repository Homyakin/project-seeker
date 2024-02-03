package ru.homyakin.seeker.game.personage.badge;

import io.vavr.control.Either;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingBadge;
import ru.homyakin.seeker.utils.models.Success;

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

    public void createDefaultPersonageBadge(PersonageId personageId) {
        getByCode(BadgeView.STANDARD.code())
            .ifPresent(badge -> badgeDao.savePersonageAvailableBadge(
                new PersonageAvailableBadge(personageId, badge, true)
            ));
    }

    public List<PersonageAvailableBadge> getPersonageAvailableBadges(PersonageId personageId) {
        return badgeDao.getPersonageAvailableBadges(personageId);
    }

    public Either<ActivatePersonageBadgeError, Success> activatePersonageBadge(PersonageId personageId, Badge badge) {
        return getPersonageAvailableBadges(personageId).stream()
            .filter(availableBadge -> Objects.equals(availableBadge.badge().id(), badge.id()))
            .findFirst()
            .<Either<ActivatePersonageBadgeError, Success>>map(availableBadge -> {
                if (availableBadge.isActive()) {
                    return Either.left(ActivatePersonageBadgeError.AlreadyActivated.INSTANCE);
                } else {
                    badgeDao.activatePersonageBadge(personageId, badge);
                    return Either.right(Success.INSTANCE);
                }
            })
            .orElseGet(() -> Either.left(ActivatePersonageBadgeError.BadgeIsNotAvailable.INSTANCE));
    }

    public Optional<Badge> getByCode(String code) {
        return badgeDao.getByCode(code);
    }

    public Optional<Badge> getById(int id) {
        return badgeDao.getById(id);
    }
}
