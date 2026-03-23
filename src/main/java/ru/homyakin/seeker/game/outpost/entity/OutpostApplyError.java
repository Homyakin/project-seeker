package ru.homyakin.seeker.game.outpost.entity;

public sealed interface OutpostApplyError {
    enum NoGroup implements OutpostApplyError { INSTANCE }

    enum NotAdmin implements OutpostApplyError { INSTANCE }

    enum UnknownBuilding implements OutpostApplyError { INSTANCE }

    enum NoOffer implements OutpostApplyError { INSTANCE }

    enum Busy implements OutpostApplyError { INSTANCE }
}
