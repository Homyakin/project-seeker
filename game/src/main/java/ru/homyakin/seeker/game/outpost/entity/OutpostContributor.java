package ru.homyakin.seeker.game.outpost.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public record OutpostContributor(PersonageId personageId, int materials) {
}
