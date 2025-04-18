package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
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

    public LeaveGroupMemberCommand(
        GroupPersonageStorage groupPersonageStorage,
        GroupStorage groupStorage,
        GroupConfig config
    ) {
        this.groupPersonageStorage = groupPersonageStorage;
        this.groupStorage = groupStorage;
        this.config = config;
    }

    /**
     * @return Возвращает таймаут на вступление в случае успеха
     */
    public Either<LeaveGroupMemberError, Duration> execute(PersonageId personageId, GroupId groupId) {
        final var personageMemberGroup = groupPersonageStorage.getPersonageMemberGroup(personageId);
        if (!personageMemberGroup.isGroupMember(groupId)) {
            return Either.left(LeaveGroupMemberError.NotGroupMember.INSTANCE);
        }

        final var memberCount = groupStorage.memberCount(groupId);
        if (memberCount == 1) {
            return Either.left(LeaveGroupMemberError.LastMember.INSTANCE);
        } else {
            groupPersonageStorage.clearMemberGroup(personageId, TimeUtils.moscowTime());
        }
        return Either.right(config.personageJoinGroupTimeout());
    }
}
