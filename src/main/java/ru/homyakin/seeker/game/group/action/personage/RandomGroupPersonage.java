package ru.homyakin.seeker.game.group.action.personage;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

public interface RandomGroupPersonage {
    Optional<PersonageId> random(GroupId groupId);
}
