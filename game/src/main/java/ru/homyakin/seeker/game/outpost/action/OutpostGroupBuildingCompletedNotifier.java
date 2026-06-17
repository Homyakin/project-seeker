package ru.homyakin.seeker.game.outpost.action;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingCompletion;

public interface OutpostGroupBuildingCompletedNotifier {
    void notifyGroup(GroupId groupId, Iterable<OutpostBuildingCompletion> completions);
}
