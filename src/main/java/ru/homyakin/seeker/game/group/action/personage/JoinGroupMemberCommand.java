package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.JoinGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class JoinGroupMemberCommand {
    private final GroupPersonageStorage groupPersonageStorage;
    private final GroupStorage groupStorage;

    public JoinGroupMemberCommand(GroupPersonageStorage groupPersonageStorage, GroupStorage groupStorage) {
        this.groupPersonageStorage = groupPersonageStorage;
        this.groupStorage = groupStorage;
    }

    public Either<JoinGroupMemberError, Group> execute(GroupId groupId, PersonageId personageId) {
        final var personageGroup = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (personageGroup.isPresent()) {
            if (personageGroup.get().equals(groupId)) {
                return Either.left(JoinGroupMemberError.PersonageAlreadyInGroup.INSTANCE);
            } else {
                return Either.left(JoinGroupMemberError.PersonageInAnotherGroup.INSTANCE);
            }
        }
        final var group = groupStorage.get(groupId).orElseThrow();
        if (!group.isRegistered()) {
            return Either.left(JoinGroupMemberError.GroupNotRegistered.INSTANCE);
        }

        groupPersonageStorage.setMemberGroup(personageId, groupId);
        return Either.right(group);
    }
}
