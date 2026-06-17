package ru.homyakin.seeker.game.event.world_raid.entity;

public sealed interface JoinWorldRaidError {
    enum NotFound implements JoinWorldRaidError {
        INSTANCE
    }

    enum NotEnoughEnergy implements JoinWorldRaidError {
        INSTANCE
    }

    enum NotInRegisteredGroup implements JoinWorldRaidError {
        INSTANCE
    }

    enum AlreadyJoined implements JoinWorldRaidError {
        INSTANCE
    }
}
