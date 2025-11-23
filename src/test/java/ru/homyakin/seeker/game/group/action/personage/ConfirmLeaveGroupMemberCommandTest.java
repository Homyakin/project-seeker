package ru.homyakin.seeker.game.group.action.personage;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.ConfirmLeaveGroupMemberResult;
import ru.homyakin.seeker.game.group.error.LeaveGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.test_utils.PersonageMemberGroupUtils;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

class ConfirmLeaveGroupMemberCommandTest {
    private static final GroupConfig groupConfig = Mockito.mock();
    private final GroupPersonageStorage groupPersonageStorage = Mockito.mock();
    private final GroupStorage groupStorage = Mockito.mock();
    private final ConfirmLeaveGroupMemberCommand confirmLeaveGroupMemberCommand = new ConfirmLeaveGroupMemberCommand(
        groupPersonageStorage,
        groupStorage,
        groupConfig
    );

    @BeforeAll
    public static void init() {
        Mockito.when(groupConfig.personageJoinGroupTimeout()).thenReturn(joinTimeout);
    }

    @Test
    void When_PersonageNotInGroup_Then_ReturnNotGroupMemberError() {
        final var personageId = new PersonageId(1);
        final var groupId = new GroupId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());

        final var result = confirmLeaveGroupMemberCommand.execute(personageId, groupId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(LeaveGroupMemberError.NotGroupMember.INSTANCE, result.getLeft());
    }

    @Test
    void When_GroupHasNoMoreMembers_Then_ReturnLastMemberLeaveAndDeleteGroupTag() {
        final var personageId = new PersonageId(1);
        final var groupId = new GroupId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        Mockito.when(groupStorage.memberCount(groupId)).thenReturn(0);
        Mockito.doNothing().when(groupStorage).deleteTag(groupId);

        final var result = confirmLeaveGroupMemberCommand.execute(personageId, groupId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(
            new ConfirmLeaveGroupMemberResult(
                ConfirmLeaveGroupMemberResult.LeaveType.LAST_MEMBER,
                joinTimeout,
                groupId
            ),
            result.get()
        );
        Mockito.verify(groupPersonageStorage).clearMemberGroup(eq(personageId), any());
        Mockito.verify(groupStorage).deleteTag(groupId);
    }

    @Test
    void When_GroupHasAnotherMembers_Then_ReturnNotLastMember() {
        final var personageId = new PersonageId(1);
        final var groupId = new GroupId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        Mockito.when(groupStorage.memberCount(groupId)).thenReturn(2);

        final var result = confirmLeaveGroupMemberCommand.execute(personageId, groupId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(
            new ConfirmLeaveGroupMemberResult(
                ConfirmLeaveGroupMemberResult.LeaveType.NOT_LAST_MEMBER,
                joinTimeout,
                groupId
            ),
            result.get()
        );
        Mockito.verify(groupPersonageStorage).clearMemberGroup(eq(personageId), any());
    }

    private static final Duration joinTimeout = Duration.ofMinutes(1);
}
