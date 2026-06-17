package ru.homyakin.seeker.game.badge.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.badge.entity.AvailableBadge;
import ru.homyakin.seeker.game.badge.entity.PersonageBadgeStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.List;

@Component
public class PersonageBadgeService extends ManipulateBadgeService<PersonageId> {
    private final BadgeService badgeService;
    private final PersonageBadgeStorage storage;

    public PersonageBadgeService(BadgeService badgeService, PersonageBadgeStorage storage) {
        this.badgeService = badgeService;
        this.storage = storage;
    }

    public void createDefaultPersonageBadge(PersonageId personageId) {
        badgeService.getByCode(BadgeView.STANDARD.code())
            .ifPresent(badge -> storage.savePersonageAvailableBadge(
                personageId, badge.id(), true
            ));
    }

    public List<AvailableBadge> getAvailableBadges(PersonageId personageId) {
        return storage.getPersonageAvailableBadges(personageId);
    }

    @Override
    protected void activateBadgeInStorage(PersonageId id, BadgeId badgeId) {
        storage.activatePersonageBadge(id, badgeId);
    }
}
