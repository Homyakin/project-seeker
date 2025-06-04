package ru.homyakin.seeker.game.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.entity.GroupStorage;

@Component
public class CountActiveRegisteredGroupsCommand {
    private final GroupStorage storage;

    public CountActiveRegisteredGroupsCommand(GroupStorage storage) {
        this.storage = storage;
    }

    public long execute() {
        return storage.countActiveRegisteredGroups();
    }
}
