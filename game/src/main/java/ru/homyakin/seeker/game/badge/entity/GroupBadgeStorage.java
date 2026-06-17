package ru.homyakin.seeker.game.badge.entity;

import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.common.models.GroupId;

import java.util.List;

public interface GroupBadgeStorage {
    List<AvailableBadge> getGroupAvailableBadges(GroupId groupId);

    void activateGroupBadge(GroupId groupId, BadgeId badgeId);
}
