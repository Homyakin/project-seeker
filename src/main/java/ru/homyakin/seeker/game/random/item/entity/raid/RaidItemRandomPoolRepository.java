package ru.homyakin.seeker.game.random.item.entity.raid;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemRandomPool;

public interface RaidItemRandomPoolRepository {
    FullItemRandomPool get(PersonageId personageId);

    void save(PersonageId personageId, FullItemRandomPool raidItemRandomPool);
}
