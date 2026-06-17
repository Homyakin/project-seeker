package ru.homyakin.seeker.game.group.action.personage;

import java.time.Duration;

import org.springframework.stereotype.Component;

import io.vavr.control.Either;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.KickGroupMemberError;
import ru.homyakin.seeker.game.group.error.LeaveGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class KickGroupMemberCommand {
    private final GroupPersonageStorage groupPersonageStorage;
    private final CheckGroupPersonage checkGroupPersonage;
    private final LeaveGroupMemberCommand leaveGroupMemberCommand;

    public KickGroupMemberCommand(
        GroupPersonageStorage groupPersonageStorage,
        CheckGroupPersonage checkGroupPersonage,
        LeaveGroupMemberCommand leaveGroupMemberCommand
    ) {
        this.groupPersonageStorage = groupPersonageStorage;
        this.checkGroupPersonage = checkGroupPersonage;
        this.leaveGroupMemberCommand = leaveGroupMemberCommand;
    }

    public Either<KickGroupMemberError, Success> prepare(
        GroupId groupId,
        PersonageId adminPersonageId,
        PersonageId targetPersonageId
    ) {
        return prepareChecks(groupId, adminPersonageId, targetPersonageId);
    }

    public Either<KickGroupMemberError, Duration> kick(
        GroupId groupId,
        PersonageId adminPersonageId,
        PersonageId targetPersonageId
    ) {
        return prepareChecks(groupId, adminPersonageId, targetPersonageId)
            .flatMap(ignored -> leaveGroupMemberCommand
                .leaveAssumingMemberOfGroup(targetPersonageId, groupId)
                .mapLeft(this::mapLeaveErrorToKick));
    }

    private KickGroupMemberError mapLeaveErrorToKick(LeaveGroupMemberError error) {
        return switch (error) {
            case LeaveGroupMemberError.NotGroupMember _ -> KickGroupMemberError.TargetNotInGroup.INSTANCE;
            case LeaveGroupMemberError.LastMember _ -> KickGroupMemberError.KickLeaveInvariantViolated.INSTANCE;
        };
    }

    private Either<KickGroupMemberError, Success> prepareChecks(
        GroupId groupId,
        PersonageId adminPersonageId,
        PersonageId targetPersonageId
    ) {
        if (adminPersonageId.equals(targetPersonageId)) {
            return Either.left(KickGroupMemberError.CannotKickSelf.INSTANCE);
        }
        final var adminMembership = groupPersonageStorage.getPersonageMemberGroup(adminPersonageId);
        if (!adminMembership.isGroupMember(groupId)) {
            return Either.left(KickGroupMemberError.PersonageNotInGroup.INSTANCE);
        }
        if (!checkGroupPersonage.isAdminInGroup(groupId, adminPersonageId)) {
            return Either.left(KickGroupMemberError.NotAnAdmin.INSTANCE);
        }
        final var targetMembership = groupPersonageStorage.getPersonageMemberGroup(targetPersonageId);
        if (!targetMembership.isGroupMember(groupId)) {
            return Either.left(KickGroupMemberError.TargetNotInGroup.INSTANCE);
        }
        return Either.right(Success.INSTANCE);
    }
}
