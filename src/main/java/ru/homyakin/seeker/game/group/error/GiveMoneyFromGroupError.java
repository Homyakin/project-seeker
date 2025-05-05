package ru.homyakin.seeker.game.group.error;

public sealed interface GiveMoneyFromGroupError permits CheckGroupMemberAdminError,
    GiveMoneyFromGroupError.GroupNotRegistered,
    GiveMoneyFromGroupError.InvalidAmount,
    GiveMoneyFromGroupError.NotEnoughMoney,
    GiveMoneyFromGroupError.PersonageNotMember {
    enum NotEnoughMoney implements GiveMoneyFromGroupError {
        INSTANCE
    }

    enum PersonageNotMember implements GiveMoneyFromGroupError {
        INSTANCE
    }

    enum GroupNotRegistered implements GiveMoneyFromGroupError {
        INSTANCE
    }

    enum InvalidAmount implements GiveMoneyFromGroupError {
        INSTANCE
    }
}
