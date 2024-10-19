package ru.homyakin.seeker.game.group.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;

import java.time.LocalDateTime;

@Component
public class UpdateGroupParameters {
    private final GroupStorage storage;

    public UpdateGroupParameters(GroupStorage storage) {
        this.storage = storage;
    }

    public void updateNextRumorDate(GroupId groupId, LocalDateTime nextRumorDate) {
        storage.updateNextRumorDate(groupId, nextRumorDate);
    }

    public void updateNextEventDate(GroupId groupId, LocalDateTime nextEventDate) {
        storage.updateNextEventDate(groupId, nextEventDate);
    }
}
