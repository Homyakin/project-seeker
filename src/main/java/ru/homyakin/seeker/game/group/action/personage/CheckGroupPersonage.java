package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import ru.homyakin.seeker.common.models.Error;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface CheckGroupPersonage {
    Either<Error, Boolean> stillInGroup(GroupId groupId, PersonageId personageId);

    boolean isAdminInGroup(GroupId groupId, PersonageId personageId);
}
