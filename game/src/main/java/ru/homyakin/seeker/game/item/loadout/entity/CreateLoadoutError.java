package ru.homyakin.seeker.game.item.loadout.entity;

public sealed interface CreateLoadoutError {
    record InvalidName(LoadoutNameError nameError) implements CreateLoadoutError {
    }

    enum MaxLoadoutsReached implements CreateLoadoutError {
        INSTANCE
    }
}
