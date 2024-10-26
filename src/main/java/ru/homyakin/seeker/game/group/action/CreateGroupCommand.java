package ru.homyakin.seeker.game.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.entity.CreateGroupRequest;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupSettings;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.LocalDateTime;

@Component
public class CreateGroupCommand {
    private final GroupStorage storage;
    private final GroupConfig config;

    public CreateGroupCommand(GroupStorage storage, GroupConfig config) {
        this.storage = storage;
        this.config = config;
    }

    public Group execute(LocalDateTime initDate) {
        var request = new CreateGroupRequest(
            true,
            TextConstants.DEFAULT_GROUP_NAME,
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
