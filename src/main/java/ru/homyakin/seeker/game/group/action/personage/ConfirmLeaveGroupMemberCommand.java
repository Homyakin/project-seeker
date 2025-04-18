package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.ConfirmLeaveGroupMemberResult;
import ru.homyakin.seeker.game.group.error.LeaveGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class ConfirmLeaveGroupMemberCommand {
    private final GroupPersonageStorage groupPersonageStorage;
    private final GroupStorage groupStorage;
    private final GroupConfig config;

    public ConfirmLeaveGroupMemberCommand(
        GroupPersonageStorage groupPersonageStorage,
        GroupStorage groupStorage,
        GroupConfig config
    ) {
        this.groupPersonageStorage = groupPersonageStorage;
        this.groupStorage = groupStorage;
        this.config = config;
    }

    public Either<LeaveGroupMemberError.NotGroupMember, ConfirmLeaveGroupMemberResult> execute(
        PersonageId personageId,
        GroupId groupId
    ) {
        final var personageMemberGroup = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (!personageMemberGroup.isGroupMember(groupId)) {
            return Either.left(LeaveGroupMemberError.NotGroupMember.INSTANCE);
        }

        groupPersonageStorage.clearMemberGroup(personageId, TimeUtils.moscowTime());
        final var memberCount = groupStorage.memberCount(groupId);
        if (memberCount == 0) {
            groupStorage.deleteTag(groupId);
            return Either.right(
                new ConfirmLeaveGroupMemberResult(
                    ConfirmLeaveGroupMemberResult.LeaveType.LAST_MEMBER,
                    config.personageJoinGroupTimeout()
                )
            );
        } else {
            return Either.right(
                new ConfirmLeaveGroupMemberResult(
                    ConfirmLeaveGroupMemberResult.LeaveType.NOT_LAST_MEMBER,
                    config.personageJoinGroupTimeout()
                )
            );
        }
    }
}
