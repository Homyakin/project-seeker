package ru.homyakin.seeker.game.group.error;

public sealed interface LeaveGroupMemberError {
    enum NotGroupMember implements LeaveGroupMemberError {
        INSTANCE
    }

    enum LastMember implements LeaveGroupMemberError {
        INSTANCE
    }
}
