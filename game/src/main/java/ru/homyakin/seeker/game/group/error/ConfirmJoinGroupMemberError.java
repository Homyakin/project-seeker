package ru.homyakin.seeker.game.group.error;

public sealed interface ConfirmJoinGroupMemberError
    permits CheckGroupMemberAdminError, JoinGroupMemberError {
}
