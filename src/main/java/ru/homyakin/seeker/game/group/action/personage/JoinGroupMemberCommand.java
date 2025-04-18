package ru.homyakin.seeker.game.group.action.personage;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.JoinGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;

@Component
public class JoinGroupMemberCommand {
    private final GroupPersonageStorage groupPersonageStorage;
    private final GroupStorage groupStorage;
    private final GroupConfig config;

    public JoinGroupMemberCommand(
        GroupPersonageStorage groupPersonageStorage,
        GroupStorage groupStorage,
        GroupConfig config
    ) {
        this.groupPersonageStorage = groupPersonageStorage;
        this.groupStorage = groupStorage;
        this.config = config;
    }

    public Either<JoinGroupMemberError, Group> execute(GroupId groupId, PersonageId personageId) {
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
        if (!group.isRegistered()) {
            return Either.left(JoinGroupMemberError.GroupNotRegistered.INSTANCE);
        }

        groupPersonageStorage.setMemberGroup(personageId, groupId);
        return Either.right(group);
    }
}
