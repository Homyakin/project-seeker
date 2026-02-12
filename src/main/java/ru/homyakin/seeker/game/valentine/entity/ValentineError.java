package ru.homyakin.seeker.game.valentine.entity;

import ru.homyakin.seeker.game.models.Money;

public sealed interface ValentineError {
    enum NotRegisteredGroup implements ValentineError {
        INSTANCE;
    }

    enum NotGroupMember implements ValentineError {
        INSTANCE;
    }

    record NotEnoughMoney(Money required) implements ValentineError {
    }

    record NotEnoughEnergy(int required) implements ValentineError {
    }

    enum TargetGroupNotFound implements ValentineError {
        INSTANCE;
    }

    enum TargetGroupNotActive implements ValentineError {
        INSTANCE;
    }

    enum TargetGroupIsEmpty implements ValentineError {
        INSTANCE;
    }

    enum CannotSendToSelf implements ValentineError {
        INSTANCE;
    }

    enum SendToThisGroup implements ValentineError {
        INSTANCE;
    }

    enum ReceiverNotRegistered implements ValentineError {
        INSTANCE;
    }

    enum ReceiverNotInTargetGroup implements ValentineError {
        INSTANCE;
    }

    enum ReceiverNotActiveInGroup implements ValentineError {
        INSTANCE;
    }

    enum InternalError implements ValentineError {
        INSTANCE;
    }
}
