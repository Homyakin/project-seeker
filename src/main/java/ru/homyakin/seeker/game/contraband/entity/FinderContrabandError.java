package ru.homyakin.seeker.game.contraband.entity;

public sealed interface FinderContrabandError {
    enum NotFound implements FinderContrabandError { INSTANCE }

    enum NotOwner implements FinderContrabandError { INSTANCE }

    enum AlreadyProcessed implements FinderContrabandError { INSTANCE }

    enum Expired implements FinderContrabandError { INSTANCE }
}
