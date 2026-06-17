package ru.homyakin.seeker.game.valentine.entity;

import java.time.LocalDateTime;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface ValentineStorage {
    void save(
        PersonageId throwingPersonageId,
        PersonageId targetPersonageId,
        boolean isRandom,
        GroupId throwingGroupId,
        GroupId targetGroupId,
        LocalDateTime date
    );

    ValentineCounts getCounts(PersonageId personageId);
}
