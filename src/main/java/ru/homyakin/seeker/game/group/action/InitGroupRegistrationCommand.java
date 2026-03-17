package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.error.GroupAlreadyRegistered;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class InitGroupRegistrationCommand {
    private final GroupStorage groupStorage;

    public InitGroupRegistrationCommand(GroupStorage groupStorage) {
        this.groupStorage = groupStorage;
    }

    public Either<GroupAlreadyRegistered, Success> execute(GroupId groupId) {
        final var group = groupStorage.get(groupId).orElseThrow();
        if (group.isRegistered()) {
            return Either.left(GroupAlreadyRegistered.INSTANCE);
        }
        return Either.right(Success.INSTANCE);
    }
}
