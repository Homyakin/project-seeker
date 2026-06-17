package ru.homyakin.seeker.game.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;

@Component
public class ChangeGroupActivity {
    private final GroupStorage storage;

    public ChangeGroupActivity(GroupStorage storage) {
        this.storage = storage;
    }

    public void activate(GroupId groupId) {
        storage.get(groupId)
            .ifPresent(group -> group.activate().peek(storage::update));
    }

    public void deactivate(GroupId groupId) {
        storage.get(groupId)
            .ifPresent(group -> group.deactivate().peek(storage::update));
    }
}
