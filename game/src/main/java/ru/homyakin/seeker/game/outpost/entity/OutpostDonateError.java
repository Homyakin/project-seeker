package ru.homyakin.seeker.game.outpost.entity;

public sealed interface OutpostDonateError {
    enum NoGroup implements OutpostDonateError {
        INSTANCE
    }

    enum BuildingNotInProgress implements OutpostDonateError {
        INSTANCE
    }

    enum ItemNotFound implements OutpostDonateError {
        INSTANCE
    }

    enum ItemEquipped implements OutpostDonateError {
        INSTANCE
    }

    enum Busy implements OutpostDonateError {
        INSTANCE
    }

    enum StateConflict implements OutpostDonateError {
        INSTANCE
    }
}
