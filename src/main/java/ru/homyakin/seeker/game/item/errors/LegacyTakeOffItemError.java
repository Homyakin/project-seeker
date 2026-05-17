package ru.homyakin.seeker.game.item.errors;

public sealed interface LegacyTakeOffItemError {
    enum PersonageMissingItem implements LegacyTakeOffItemError { INSTANCE }

    enum AlreadyTakenOff implements LegacyTakeOffItemError { INSTANCE }

    enum NotEnoughSpaceInBag implements LegacyTakeOffItemError { INSTANCE }
}
