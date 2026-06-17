package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.error.ZeroEnabledEventIntervalsError;
import ru.homyakin.seeker.game.group.error.IncorrectTimeZone;

@Component
public class EditGroupSettings {
    private final GroupStorage storage;
    private final GetGroup getGroup;

    public EditGroupSettings(GroupStorage storage, GetGroup getGroup) {
        this.storage = storage;
        this.getGroup = getGroup;
    }

    public Either<ZeroEnabledEventIntervalsError, Group> toggleEventInterval(GroupId groupId, int intervalIndex) {
        return getGroup.forceGet(groupId).toggleEventInterval(intervalIndex).peek(storage::update);
    }

    public Either<IncorrectTimeZone, Group> changeTimeZone(GroupId groupId, int timeZone) {
        return getGroup.forceGet(groupId).changeTimeZone(timeZone).peek(storage::update);
    }
}
