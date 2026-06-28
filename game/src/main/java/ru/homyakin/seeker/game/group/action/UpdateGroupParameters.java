package ru.homyakin.seeker.game.group.action;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;

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

    public void updateRaidLevel(GroupId groupId, boolean wasRaidSuccess) {
        final var group = storage.get(groupId).orElseThrow();
        final var newRaidLevel = Math.min(
            25,
            Math.max(1, group.raidLevel() + (wasRaidSuccess ? 1 : -2))
        );
        storage.updateRaidLevel(groupId, newRaidLevel);
    }
}
