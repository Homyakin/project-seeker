package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.GroupTaxService;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.ConfirmJoinGroupMemberError;
import ru.homyakin.seeker.game.group.error.JoinGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class JoinGroupMemberCommand {
    private final GroupPersonageStorage groupPersonageStorage;
    private final CheckGroupMemberAdminCommand checkGroupMemberAdminCommand;
    private final CheckGroupPersonage checkGroupPersonage;
    private final GroupStorage groupStorage;
    private final GroupConfig config;
    private final GroupTaxService groupTaxService;

    public JoinGroupMemberCommand(
        GroupPersonageStorage groupPersonageStorage,
        CheckGroupMemberAdminCommand checkGroupMemberAdminCommand,
        CheckGroupPersonage checkGroupPersonage,
        GroupStorage groupStorage,
        GroupConfig config,
        GroupTaxService groupTaxService
    ) {
        this.groupPersonageStorage = groupPersonageStorage;
        this.checkGroupMemberAdminCommand = checkGroupMemberAdminCommand;
        this.checkGroupPersonage = checkGroupPersonage;
        this.groupStorage = groupStorage;
        this.config = config;
        this.groupTaxService = groupTaxService;
    }

    @Transactional
    public Either<JoinGroupMemberError, Group> join(GroupId groupId, PersonageId personageId) {
        return canJoin(groupId, personageId)
            .peek(_ -> groupTaxService.applyJoin(groupId, personageId))
            .peek(_ -> groupPersonageStorage.setMemberGroup(personageId, groupId));
    }

    @Transactional
    public Either<ConfirmJoinGroupMemberError, Success> confirm(
        GroupId groupId,
        PersonageId confirmingPersonageId,
        PersonageId joiningPersonageId
    ) {
        final var canJoinResult = canJoin(groupId, joiningPersonageId);
        if (canJoinResult.isLeft() && canJoinResult.getLeft() instanceof JoinGroupMemberError.ConfirmationRequired) {
            return checkGroupMemberAdminCommand.execute(groupId, confirmingPersonageId)
                .peek(_ -> groupTaxService.applyJoin(groupId, joiningPersonageId))
                .peek(_ -> groupPersonageStorage.setMemberGroup(joiningPersonageId, groupId))
                .mapLeft(it -> it);
        }
        // Нужен маппинг left иначе система типов не понимает
        return canJoinResult
            .peek(_ -> groupTaxService.applyJoin(groupId, joiningPersonageId))
            .peek(_ -> groupPersonageStorage.setMemberGroup(joiningPersonageId, groupId))
            .map(_ -> Success.INSTANCE)
            .mapLeft(it -> it);
    }

    private Either<JoinGroupMemberError, Group> canJoin(GroupId groupId, PersonageId personageId) {
        final var personageMemberGroup = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (personageMemberGroup.isGroupMember(groupId)) {
            return Either.left(JoinGroupMemberError.PersonageAlreadyInGroup.INSTANCE);
        } else if (personageMemberGroup.hasGroup()) {
            return Either.left(JoinGroupMemberError.PersonageInAnotherGroup.INSTANCE);
        }
        final var remainJoinTimeout = personageMemberGroup.remainJoinTimeout(config.personageJoinGroupTimeout());
        if (remainJoinTimeout.isPresent()) {
            return Either.left(new JoinGroupMemberError.PersonageJoinTimeout(remainJoinTimeout.get()));
        }
        final var group = groupStorage.get(groupId).orElseThrow();
        if (!checkGroupPersonage.isAdminInGroup(groupId, personageId)) {
            return Either.left(JoinGroupMemberError.ConfirmationRequired.INSTANCE);
        }
        return Either.right(group);
    }
}
