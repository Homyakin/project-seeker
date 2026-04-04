package ru.homyakin.seeker.game.outpost.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public record OutpostMaterialContributor(PersonageId personageId, int materials) {
}
