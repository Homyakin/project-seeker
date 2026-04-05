package ru.homyakin.seeker.game.outpost.entity;

import java.util.List;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface OutpostBuildingContributionStorage {
    void add(GroupId groupId, Building building, PersonageId personageId, int materialsDelta);

    List<OutpostContributor> listTop(GroupId groupId, Building building, int limit);

    void clear(GroupId groupId, Building building);
}
