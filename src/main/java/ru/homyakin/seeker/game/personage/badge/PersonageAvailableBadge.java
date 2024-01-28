package ru.homyakin.seeker.game.personage.badge;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public record PersonageAvailableBadge(
    PersonageId personageId,
    Badge badge,
    boolean isActive
) {

}
