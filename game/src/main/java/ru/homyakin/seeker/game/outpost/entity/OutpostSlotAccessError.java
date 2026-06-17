package ru.homyakin.seeker.game.outpost.entity;

public sealed interface OutpostSlotAccessError {
    enum NoGroup implements OutpostSlotAccessError {
        INSTANCE
    }

    enum NotFound implements OutpostSlotAccessError {
        INSTANCE
    }
}
