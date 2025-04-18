package ru.homyakin.seeker.game.group.error;

import java.time.Duration;

public record ConfirmLeaveGroupMemberResult(
    LeaveType leaveType,
    Duration joinTimeout
) {
    public enum LeaveType {
        NOT_LAST_MEMBER,
        LAST_MEMBER,
    }
}
