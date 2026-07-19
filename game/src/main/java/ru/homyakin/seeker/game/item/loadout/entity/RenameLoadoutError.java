package ru.homyakin.seeker.game.item.loadout.entity;

public sealed interface RenameLoadoutError {
    record InvalidName(LoadoutNameError nameError) implements RenameLoadoutError {
    }

    enum LoadoutNotFound implements RenameLoadoutError {
        INSTANCE
    }
}
