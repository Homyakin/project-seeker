package ru.homyakin.seeker.game.item.loadout.entity;

public sealed interface SaveLoadoutError {
    enum LoadoutNotFound implements SaveLoadoutError {
        INSTANCE
    }
}
