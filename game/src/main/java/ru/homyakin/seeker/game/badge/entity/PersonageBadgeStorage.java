package ru.homyakin.seeker.game.badge.entity;

import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.List;

public interface PersonageBadgeStorage {
    void savePersonageAvailableBadge(PersonageId personageId, BadgeId badgeId, boolean isActive);

    List<AvailableBadge> getPersonageAvailableBadges(PersonageId personageId);

    void activatePersonageBadge(PersonageId personageId, BadgeId badgeId);
}
