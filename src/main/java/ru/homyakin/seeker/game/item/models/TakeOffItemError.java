package ru.homyakin.seeker.game.item.models;

public sealed interface TakeOffItemError {
    enum PersonageMissingItem implements TakeOffItemError { INSTANCE }

    enum AlreadyTakenOff implements TakeOffItemError { INSTANCE }

    enum NotEnoughSpaceInBag implements TakeOffItemError { INSTANCE }
}
