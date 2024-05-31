package ru.homyakin.seeker.game.item.errors;

public sealed interface DropItemError {
    enum PersonageMissingItem implements DropItemError { INSTANCE }
}
