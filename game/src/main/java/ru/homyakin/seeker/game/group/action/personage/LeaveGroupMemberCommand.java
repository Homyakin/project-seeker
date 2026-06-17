package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.GroupTaxService;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.LeaveGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;

@Component
public class LeaveGroupMemberCommand {
    private final GroupPersonageStorage groupPersonageStorage;
    private final GroupStorage groupStorage;
    private final GroupConfig config;
    private final GroupTaxService groupTaxService;

    public LeaveGroupMemberCommand(
        GroupPersonageStorage groupPersonageStorage,
        GroupStorage groupStorage,
        GroupConfig config,
        GroupTaxService groupTaxService
    ) {
        this.groupPersonageStorage = groupPersonageStorage;
        this.groupStorage = groupStorage;
        this.config = config;
        this.groupTaxService = groupTaxService;
    }

    /**
     * @return Возвращает таймаут на вступление в случае успеха
     */
    @Transactional
    public Either<LeaveGroupMemberError, Duration> execute(PersonageId personageId, GroupId groupId) {
        final var personageMemberGroup = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (!personageMemberGroup.isGroupMember(groupId)) {
            return Either.left(LeaveGroupMemberError.NotGroupMember.INSTANCE);
        }
        return leave(personageId, groupId);
    }

    /**
     * @return Возвращает таймаут на вступление в случае успеха
     */
    @Transactional
    public Either<LeaveGroupMemberError, Duration> execute(PersonageId personageId) {
        final var personageMemberGroup = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (!personageMemberGroup.hasGroup()) {
            return Either.left(LeaveGroupMemberError.NotGroupMember.INSTANCE);
        }
        final var groupId = personageMemberGroup.groupId().get();
        return leave(personageId, groupId);
    }

    /**
     * Same as {@link #execute(PersonageId, GroupId)} after membership in {@code groupId} is already validated.
     */
    @Transactional
    public Either<LeaveGroupMemberError, Duration> leaveAssumingMemberOfGroup(PersonageId personageId, GroupId groupId) {
        return leave(personageId, groupId);
    }

    private Either<LeaveGroupMemberError, Duration> leave(PersonageId personageId, GroupId groupId) {
        final var memberCount = groupStorage.memberCount(groupId);
        final var group = groupStorage.get(groupId).orElseThrow();
        if (memberCount == 1) {
            if (group.isRegistered()) {
                return Either.left(LeaveGroupMemberError.LastMember.INSTANCE);
            }
            groupTaxService.applyLeave(groupId, personageId);
            groupPersonageStorage.clearMemberGroup(personageId, TimeUtils.moscowTime());
            if (groupStorage.memberCount(groupId) == 0) {
                groupStorage.deleteTag(groupId);
            }
        } else {
            groupTaxService.applyLeave(groupId, personageId);
            groupPersonageStorage.clearMemberGroup(personageId, TimeUtils.moscowTime());
        }
        return Either.right(config.personageJoinGroupTimeout());
    }
}
