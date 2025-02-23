package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.error.GroupAlreadyRegistered;
import ru.homyakin.seeker.game.models.Money;

@Component
public class InitGroupRegistrationCommand {
    private final GroupStorage groupStorage;
    private final GroupConfig config;

    public InitGroupRegistrationCommand(GroupStorage groupStorage, GroupConfig config) {
        this.groupStorage = groupStorage;
        this.config = config;
    }

    /**
     * @return необходимое количество денег для регистрации группы
     */
    public Either<GroupAlreadyRegistered, Money> execute(GroupId groupId) {
        final var group = groupStorage.get(groupId).orElseThrow();
        if (group.isRegistered()) {
            return Either.left(GroupAlreadyRegistered.INSTANCE);
        }
        return Either.right(config.registrationPrice());
    }
}
