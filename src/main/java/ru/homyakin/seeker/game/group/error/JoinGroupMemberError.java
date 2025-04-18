package ru.homyakin.seeker.game.group.error;

import java.time.Duration;

public sealed interface JoinGroupMemberError {
    enum PersonageAlreadyInGroup implements JoinGroupMemberError {
        INSTANCE
    }

    enum PersonageInAnotherGroup implements JoinGroupMemberError {
        INSTANCE
    }

    record PersonageJoinTimeout(Duration remain) implements JoinGroupMemberError {
    }

    enum GroupNotRegistered implements JoinGroupMemberError {
        INSTANCE
    }
}
