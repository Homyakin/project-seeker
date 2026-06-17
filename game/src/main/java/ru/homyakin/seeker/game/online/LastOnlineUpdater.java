package ru.homyakin.seeker.game.online;

import java.time.LocalDateTime;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface LastOnlineUpdater {
    void touchPersonage(PersonageId personageId, LocalDateTime at);

    void touchGroup(GroupId groupId, LocalDateTime at);

    void touchActiveMembership(GroupId groupId, PersonageId personageId, LocalDateTime at);
}
