package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPool;

public interface RaidItemRandomPoolRepository {
    ItemRandomPool get(PersonageId personageId);

    void save(PersonageId personageId, ItemRandomPool raidItemRandomPool);
}
