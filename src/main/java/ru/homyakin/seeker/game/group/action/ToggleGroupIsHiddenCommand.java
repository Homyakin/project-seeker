package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.error.ForbiddenToggleHidden;

@Component
public class ToggleGroupIsHiddenCommand {
    private final GroupStorage storage;

    public ToggleGroupIsHiddenCommand(GroupStorage storage) {
        this.storage = storage;
    }

    /**
     * @return в случае успеха возвращает новое значение
     */
    public Either<ForbiddenToggleHidden, Boolean> execute(GroupId groupId) {
        final var group = storage.get(groupId).orElseThrow();
        if (!group.settings().enableToggleHide()) {
            return Either.left(ForbiddenToggleHidden.INSTANCE);
        }
        return Either.right(storage.toggleIsHidden(groupId));
    }
}
