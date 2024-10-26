package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.utils.NameError;
import ru.homyakin.seeker.game.utils.NameValidator;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class ChangeGroupNameCommand {
    private final GroupStorage storage;

    public ChangeGroupNameCommand(GroupStorage storage) {
        this.storage = storage;
    }

    public Either<NameError, Success> execute(GroupId groupId, String name) {
        final var validationResult = NameValidator.validateName(name);
        if (validationResult.isLeft()) {
            return Either.left(validationResult.getLeft());
        }
        storage.changeGroupName(groupId, validationResult.get());
        return Either.right(Success.INSTANCE);
    }
}
