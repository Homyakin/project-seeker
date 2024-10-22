package ru.homyakin.seeker.game.stats.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface GroupPersonageClient {
    void create(GroupId groupId, PersonageId personageId);
}
