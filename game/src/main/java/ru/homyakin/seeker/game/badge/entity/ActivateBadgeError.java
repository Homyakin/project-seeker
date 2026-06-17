package ru.homyakin.seeker.game.badge.entity;

public sealed interface ActivateBadgeError {
    enum AlreadyActivated implements ActivateBadgeError { INSTANCE }

    enum BadgeIsNotAvailable implements ActivateBadgeError { INSTANCE }
}
