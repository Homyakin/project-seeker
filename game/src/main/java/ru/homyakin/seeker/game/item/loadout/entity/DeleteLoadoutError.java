package ru.homyakin.seeker.game.item.loadout.entity;

public sealed interface DeleteLoadoutError {
    enum LoadoutNotFound implements DeleteLoadoutError {
        INSTANCE
    }
}
