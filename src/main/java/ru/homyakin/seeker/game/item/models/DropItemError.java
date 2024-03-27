package ru.homyakin.seeker.game.item.models;

public sealed interface DropItemError {
    enum PersonageMissingItem implements DropItemError { INSTANCE }
}
