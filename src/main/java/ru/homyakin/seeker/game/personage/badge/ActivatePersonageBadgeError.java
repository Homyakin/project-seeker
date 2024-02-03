package ru.homyakin.seeker.game.personage.badge;

public sealed interface ActivatePersonageBadgeError {
    enum AlreadyActivated implements ActivatePersonageBadgeError { INSTANCE }

    enum BadgeIsNotAvailable implements ActivatePersonageBadgeError { INSTANCE }
}
