package ru.homyakin.seeker.game.group.error;

public sealed interface KickGroupMemberError {
    enum NotAnAdmin implements KickGroupMemberError {
        INSTANCE
    }

    enum PersonageNotInGroup implements KickGroupMemberError {
        INSTANCE
    }

    enum CannotKickSelf implements KickGroupMemberError {
        INSTANCE
    }

    enum TargetNotInGroup implements KickGroupMemberError {
        INSTANCE
    }

    /** Target was the only member left in a registered group — must not happen when admin is another member. */
    enum KickLeaveInvariantViolated implements KickGroupMemberError {
        INSTANCE
    }
}
