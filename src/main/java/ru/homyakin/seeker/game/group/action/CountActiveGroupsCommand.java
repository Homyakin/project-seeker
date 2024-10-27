package ru.homyakin.seeker.game.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.entity.GroupStorage;

@Component
public class CountActiveGroupsCommand {
    private final GroupStorage storage;

    public CountActiveGroupsCommand(GroupStorage storage) {
        this.storage = storage;
    }

    public long execute() {
        return storage.countActiveGroups(REQUIRED_ACTIVE_PERSONAGES);
    }

    private static final int REQUIRED_ACTIVE_PERSONAGES = 2;
}
