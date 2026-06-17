package ru.homyakin.seeker.game.group.entity;

import ru.homyakin.seeker.common.models.GroupId;

import java.util.List;
import java.util.Optional;

public interface GroupBattleResultStorage {
    void saveBatch(List<SavedGroupBattleResult> results);

    Optional<SavedGroupBattleResult> getBattleResult(GroupId groupId, long launchedEventId);
}
