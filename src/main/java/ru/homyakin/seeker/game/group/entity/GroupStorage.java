package ru.homyakin.seeker.game.group.entity;

import ru.homyakin.seeker.common.models.GroupId;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface GroupStorage {
    GroupId create(CreateGroupRequest request);

    long countActiveRegisteredGroups(LocalDateTime start);

    Optional<Group> get(GroupId groupId);

    List<Group> getGetGroupsWithLessNextEventDate(LocalDateTime maxNextEventDate);

    List<Group> getGetGroupsWithLessNextRumorDate(LocalDateTime maxNextRumorDate);

    void update(Group group);

    void updateNextEventDate(GroupId groupId, LocalDateTime nextEventDate);

    void updateNextRumorDate(GroupId groupId, LocalDateTime nextRumorDate);

    void updateRaidLevel(GroupId groupId, int raidLevel);

    void changeGroupName(GroupId groupId, String name);

    /**
     * @return возвращает новое значение
     */
    boolean toggleIsHidden(GroupId groupId);

    void setTag(GroupId groupId, String tag);

    int memberCount(GroupId groupId);

    void deleteTag(GroupId groupId);

    boolean isTagExists(String tag);

    Optional<GroupProfile> getProfile(GroupId groupId);

    Optional<Group> getByTag(String tag);

    List<Group> getByTags(List<String> tags);
}
