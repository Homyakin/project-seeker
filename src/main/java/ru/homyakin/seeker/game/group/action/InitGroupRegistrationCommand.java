package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.InitRegistrationInfo;
import ru.homyakin.seeker.game.group.error.GroupAlreadyRegistered;

@Component
public class InitGroupRegistrationCommand {


    private final GroupStorage groupStorage;

    public InitGroupRegistrationCommand(GroupStorage groupStorage) {
        this.groupStorage = groupStorage;
    }

    public Either<GroupAlreadyRegistered, InitRegistrationInfo> execute(GroupId groupId) {
        final var group = groupStorage.get(groupId).orElseThrow();
        if (group.isRegistered()) {
            return Either.left(GroupAlreadyRegistered.INSTANCE);
        }
        return Either.right(new InitRegistrationInfo(GroupTagService.MIN_MONOLITH_LEVEL_FOR_REGISTRATION));
    }
}
