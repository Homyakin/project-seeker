package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface PersonageStatsStorage {
    PersonageStats get(PersonageId personageId);
}
