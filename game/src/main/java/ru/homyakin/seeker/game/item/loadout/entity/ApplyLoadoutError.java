package ru.homyakin.seeker.game.item.loadout.entity;

import java.util.List;

public sealed interface ApplyLoadoutError {
    enum LoadoutNotFound implements ApplyLoadoutError {
        INSTANCE
    }

    record MissingItems(List<Long> missingItemIds) implements ApplyLoadoutError {
    }

    enum NotEnoughSpaceInBag implements ApplyLoadoutError {
        INSTANCE
    }

    enum ConflictingSlots implements ApplyLoadoutError {
        INSTANCE
    }
}
