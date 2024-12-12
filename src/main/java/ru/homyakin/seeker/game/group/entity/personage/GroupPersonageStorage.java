package ru.homyakin.seeker.game.group.entity.personage;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;
import java.util.Set;

public interface GroupPersonageStorage {
    Optional<PersonageId> randomPersonage(GroupId groupId);

    int countPersonages(GroupId groupId);

    void deactivatePersonageInGroup(GroupId groupId, PersonageId personageId);

    void createOrActivate(GroupId groupId, PersonageId personageId);

    void create(GroupId groupId, PersonageId personageId);

    Set<PersonageId> getActiveGroupPersonages(GroupId groupId);
}
