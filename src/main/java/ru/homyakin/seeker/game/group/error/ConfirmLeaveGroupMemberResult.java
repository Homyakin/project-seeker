package ru.homyakin.seeker.game.group.error;

import ru.homyakin.seeker.common.models.GroupId;

import java.time.Duration;

public record ConfirmLeaveGroupMemberResult(
    LeaveType leaveType,
    Duration joinTimeout,
    GroupId groupId
) {
    public enum LeaveType {
        NOT_LAST_MEMBER,
        LAST_MEMBER,
    }
}
