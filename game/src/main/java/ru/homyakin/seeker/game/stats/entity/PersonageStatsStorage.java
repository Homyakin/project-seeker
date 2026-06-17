package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;

import java.util.Optional;

public interface PersonageStatsStorage {
    Optional<PersonageStats> get(PersonageId personageId, SeasonNumber seasonNumber);
    
    void add(AddPersonageStats stats);
}
