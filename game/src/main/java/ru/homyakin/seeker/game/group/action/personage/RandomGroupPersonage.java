package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.Error;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

@Component
public class RandomGroupPersonage {
    private final GroupPersonageStorage storage;
    private final CheckGroupPersonage checkGroupPersonage;

    public RandomGroupPersonage(GroupPersonageStorage storage, CheckGroupPersonage checkGroupPersonage) {
        this.storage = storage;
        this.checkGroupPersonage = checkGroupPersonage;
    }

    public Either<Error, Optional<PersonageId>> randomMember(GroupId groupId) {
        PersonageId personageId = null;
        do {
            final var optionalResult = storage.randomMember(groupId);
            if (optionalResult.isEmpty()) {
                return Either.right(Optional.empty());
            }
            final var isInGroup = checkGroupPersonage.stillInGroup(groupId, optionalResult.get());
            if (isInGroup.isLeft()) {
                return Either.left(isInGroup.getLeft());
            }
            if (isInGroup.isRight()) {
                if (isInGroup.get()) {
                    personageId = optionalResult.get();
                } else {
                    storage.deactivatePersonageInGroup(groupId, optionalResult.get());
                }
            }
        } while (personageId == null);
        return Either.right(Optional.of(personageId));
    }
}
