package ru.homyakin.seeker.game.group.entity.personage;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface GroupPersonageStorage {
    Optional<PersonageId> randomMember(GroupId groupId);

    int countPersonageMembers(GroupId groupId);

    void deactivatePersonageInGroup(GroupId groupId, PersonageId personageId);

    void createOrActivate(GroupId groupId, PersonageId personageId);

    void create(GroupId groupId, PersonageId personageId);

    Set<PersonageId> getActiveGroupPersonages(GroupId groupId);

    boolean isPersonageActiveInGroup(GroupId groupId, PersonageId personageId);

    PersonageMemberGroup getPersonageMemberGroup(PersonageId personageId);

    void setMemberGroup(PersonageId personageId, GroupId groupId);

    void clearMemberGroup(PersonageId personageId, LocalDateTime now);
}
