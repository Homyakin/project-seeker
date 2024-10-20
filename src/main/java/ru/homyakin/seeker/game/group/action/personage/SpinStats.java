package ru.homyakin.seeker.game.group.action.personage;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface SpinStats {
    void addPersonageSpinWin(GroupId groupId, PersonageId personageId);
}
