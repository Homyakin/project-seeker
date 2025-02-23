package ru.homyakin.seeker.game.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupProfile;
import ru.homyakin.seeker.game.group.entity.GroupStorage;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class GetGroup {
    private final GroupStorage storage;

    public GetGroup(GroupStorage storage) {
        this.storage = storage;
    }

    public Group forceGet(GroupId groupId) {
        return storage.get(groupId).orElseThrow();
    }

    public GroupProfile forceGetProfile(GroupId groupId) {
        return storage.getProfile(groupId).orElseThrow();
    }

    public List<Group> getGetGroupsWithLessNextEventDate(LocalDateTime maxNextEventDate) {
        return storage.getGetGroupsWithLessNextEventDate(maxNextEventDate);
    }

    public List<Group> getGetGroupsWithLessNextRumorDate(LocalDateTime maxNextRumorDate) {
        return storage.getGetGroupsWithLessNextRumorDate(maxNextRumorDate);
    }
}
