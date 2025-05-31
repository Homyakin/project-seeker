package ru.homyakin.seeker.game.tavern_menu.order.models;

import java.time.Duration;

public sealed interface ThrowToGroupError permits
    ThrowOrderError,
    ThrowToGroupError.InternalError,
    ThrowToGroupError.NotGroupMember,
    ThrowToGroupError.NotRegisteredGroup,
    ThrowToGroupError.TargetGroupIsEmpty,
    ThrowToGroupError.TargetGroupNotFound,
    ThrowToGroupError.TargetGroupTimeout,
    ThrowToGroupError.ThrowingGroupTimeout,
    ThrowToGroupError.ThrowToThisGroup {

    enum NotRegisteredGroup implements ThrowToGroupError {
        INSTANCE;
    }

    enum NotGroupMember implements ThrowToGroupError {
        INSTANCE;
    }

    enum TargetGroupNotFound implements ThrowToGroupError {
        INSTANCE;
    }

    enum ThrowToThisGroup implements ThrowToGroupError {
        INSTANCE;
    }

    enum InternalError implements ThrowToGroupError {
        INSTANCE;
    }

    enum TargetGroupIsEmpty implements ThrowToGroupError {
        INSTANCE;
    }

    record ThrowingGroupTimeout(Duration timeout) implements ThrowToGroupError {
    }

    enum TargetGroupTimeout implements ThrowToGroupError {
        INSTANCE;
    }
}
