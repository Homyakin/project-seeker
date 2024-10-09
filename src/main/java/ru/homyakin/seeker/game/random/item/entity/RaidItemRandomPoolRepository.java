package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface RaidItemRandomPoolRepository {
    FullItemRandomPool get(PersonageId personageId);

    void save(PersonageId personageId, FullItemRandomPool raidItemRandomPool);
}
