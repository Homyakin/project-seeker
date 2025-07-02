package ru.homyakin.seeker.game.badge.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.BadgeId;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.entity.AvailableBadge;
import ru.homyakin.seeker.game.badge.entity.GroupBadgeStorage;

import java.util.List;

@Component
public class GroupBadgeService extends ManipulateBadgeService<GroupId> {
    private final GroupBadgeStorage storage;

    public GroupBadgeService(GroupBadgeStorage storage) {
        this.storage = storage;
    }

    @Override
    public List<AvailableBadge> getAvailableBadges(GroupId id) {
        return storage.getGroupAvailableBadges(id);
    }

    @Override
    protected void activateBadgeInStorage(GroupId id, BadgeId badgeId) {
        storage.activateGroupBadge(id, badgeId);
    }
}
