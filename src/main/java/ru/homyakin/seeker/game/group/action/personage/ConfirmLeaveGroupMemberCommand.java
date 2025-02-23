package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.ConfirmLeaveGroupMemberResult;
import ru.homyakin.seeker.game.group.error.LeaveGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class ConfirmLeaveGroupMemberCommand {
    private final GroupPersonageStorage groupPersonageStorage;
    private final GroupStorage groupStorage;

    public ConfirmLeaveGroupMemberCommand(GroupPersonageStorage groupPersonageStorage, GroupStorage groupStorage) {
        this.groupPersonageStorage = groupPersonageStorage;
        this.groupStorage = groupStorage;
    }

    public Either<LeaveGroupMemberError.NotGroupMember, ConfirmLeaveGroupMemberResult> execute(
        PersonageId personageId,
        GroupId groupId
    ) {
        final var personageMemberGroup = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (personageMemberGroup.isEmpty() || !personageMemberGroup.get().equals(groupId)) {
            return Either.left(LeaveGroupMemberError.NotGroupMember.INSTANCE);
        }

        groupPersonageStorage.clearMemberGroup(personageId);
        final var memberCount = groupStorage.memberCount(groupId);
        if (memberCount == 0) {
            groupStorage.deleteTag(groupId);
            return Either.right(ConfirmLeaveGroupMemberResult.LAST_MEMBER);
        } else {
            return Either.right(ConfirmLeaveGroupMemberResult.NOT_LAST_MEMBER);
        }
    }
}
