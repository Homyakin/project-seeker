package ru.homyakin.seeker.game.item.loadout.entity;

import ru.homyakin.seeker.game.utils.NameError;

public sealed interface LoadoutNameError {
    record InvalidName(NameError nameError) implements LoadoutNameError {
    }
}
