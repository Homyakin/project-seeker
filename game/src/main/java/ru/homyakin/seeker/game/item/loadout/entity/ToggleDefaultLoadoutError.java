package ru.homyakin.seeker.game.item.loadout.entity;

public sealed interface ToggleDefaultLoadoutError {
    enum LoadoutNotFound implements ToggleDefaultLoadoutError {
        INSTANCE
    }

    enum UnsupportedEventType implements ToggleDefaultLoadoutError {
        INSTANCE
    }
}
