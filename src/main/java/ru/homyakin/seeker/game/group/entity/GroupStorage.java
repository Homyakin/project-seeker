package ru.homyakin.seeker.game.group.entity;

import ru.homyakin.seeker.common.models.GroupId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GroupStorage {
    GroupId create(CreateGroupRequest request);

    Optional<Group> get(GroupId groupId);

    List<Group> getGetGroupsWithLessNextEventDate(LocalDateTime maxNextEventDate);

    List<Group> getGetGroupsWithLessNextRumorDate(LocalDateTime maxNextRumorDate);

    void update(Group group);

    void updateNextEventDate(GroupId groupId, LocalDateTime nextEventDate);

    void updateNextRumorDate(GroupId groupId, LocalDateTime nextRumorDate);

    void changeGroupName(GroupId groupId, String name);
}
