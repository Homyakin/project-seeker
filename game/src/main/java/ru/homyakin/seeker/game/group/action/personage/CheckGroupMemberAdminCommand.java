package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.CheckGroupMemberAdminError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class CheckGroupMemberAdminCommand {
    private final CheckGroupPersonage checkGroupPersonage;
    private final GroupPersonageStorage groupPersonageStorage;

    public CheckGroupMemberAdminCommand(
        CheckGroupPersonage checkGroupPersonage,
        GroupPersonageStorage groupPersonageStorage
    ) {
        this.checkGroupPersonage = checkGroupPersonage;
        this.groupPersonageStorage = groupPersonageStorage;
    }

    public Either<CheckGroupMemberAdminError, Success> execute(GroupId groupId, PersonageId personageId) {
        final var groupPersonage = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (!groupPersonage.isGroupMember(groupId)) {
            return Either.left(CheckGroupMemberAdminError.PersonageNotInGroup.INSTANCE);
        }
        if (!checkGroupPersonage.isAdminInGroup(groupId, personageId)) {
            return Either.left(CheckGroupMemberAdminError.NotAnAdmin.INSTANCE);
        }
        return Either.right(Success.INSTANCE);
    }
}
