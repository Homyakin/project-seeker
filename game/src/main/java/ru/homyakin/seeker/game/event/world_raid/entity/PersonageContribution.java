package ru.homyakin.seeker.game.event.world_raid.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public record PersonageContribution(
    PersonageId personageId,
    int contribution
) {
}
