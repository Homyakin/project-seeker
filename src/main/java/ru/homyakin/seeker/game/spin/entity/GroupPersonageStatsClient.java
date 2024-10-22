package ru.homyakin.seeker.game.spin.entity;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface GroupPersonageStatsClient {
    void addPersonageSpinWin(GroupId groupId, PersonageId personageId);
}
