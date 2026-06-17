package ru.homyakin.seeker.game.group.action.personage;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;

@Component
public class CountActivePersonagesInGroup {
    private final GroupPersonageStorage storage;

    public CountActivePersonagesInGroup(GroupPersonageStorage storage) {
        this.storage = storage;
    }

    public int count(GroupId groupId) {
        return storage.countActivePersonageMembers(groupId);
    }
}
