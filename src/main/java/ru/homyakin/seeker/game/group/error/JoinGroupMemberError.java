package ru.homyakin.seeker.game.group.error;

public sealed interface JoinGroupMemberError {
    enum PersonageAlreadyInGroup implements JoinGroupMemberError {
        INSTANCE
    }

    enum PersonageInAnotherGroup implements JoinGroupMemberError {
        INSTANCE
    }

    enum GroupNotRegistered implements JoinGroupMemberError {
        INSTANCE
    }
}
