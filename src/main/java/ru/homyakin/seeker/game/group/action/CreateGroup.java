package ru.homyakin.seeker.game.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.entity.CreateGroupRequest;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupSettings;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.LocalDateTime;

@Component
public class CreateGroup {
    private final GroupStorage storage;
    private final GroupConfig config;

    public CreateGroup(GroupStorage storage, GroupConfig config) {
        this.storage = storage;
        this.config = config;
    }

    public Group create(LocalDateTime initDate) {
        var request = new CreateGroupRequest(
            true,
            initDate,
            initDate,
            initDate.plusMinutes(RandomUtils.getInInterval(120, 240)),
            new GroupSettings(
                TimeUtils.moscowOffset(),
                config.defaultEventIntervals()
            )
        );
        final var id = storage.create(request);
        return storage.get(id).orElseThrow();
    }
}
