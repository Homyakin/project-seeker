package ru.homyakin.seeker.game.group.error;

public sealed interface CheckGroupMemberAdminError extends ConfirmJoinGroupMemberError {
    enum NotAnAdmin implements CheckGroupMemberAdminError {
        INSTANCE
    }

    enum PersonageNotInGroup implements CheckGroupMemberAdminError {
        INSTANCE
    }
}
