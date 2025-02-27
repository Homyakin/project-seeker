package ru.homyakin.seeker.game.group.action.personage;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.ConfirmLeaveGroupMemberResult;
import ru.homyakin.seeker.game.group.error.LeaveGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

class ConfirmLeaveGroupMemberCommandTest {
    private final GroupPersonageStorage groupPersonageStorage = Mockito.mock();
    private final GroupStorage groupStorage = Mockito.mock();
    private final ConfirmLeaveGroupMemberCommand confirmLeaveGroupMemberCommand = new ConfirmLeaveGroupMemberCommand(
        groupPersonageStorage,
        groupStorage
    );

    @Test
    void When_PersonageNotInGroup_Then_ReturnNotGroupMemberError() {
        final var personageId = new PersonageId(1);
        final var groupId = new GroupId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.empty());

        final var result = confirmLeaveGroupMemberCommand.execute(personageId, groupId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(LeaveGroupMemberError.NotGroupMember.INSTANCE, result.getLeft());
    }

    @Test
    void When_GroupHasNoMoreMembers_Then_ReturnLastMemberLeaveAndDeleteGroupTag() {
        final var personageId = new PersonageId(1);
        final var groupId = new GroupId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.of(groupId));
        Mockito.when(groupStorage.memberCount(groupId)).thenReturn(0);
        Mockito.doNothing().when(groupStorage).deleteTag(groupId);

        final var result = confirmLeaveGroupMemberCommand.execute(personageId, groupId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(ConfirmLeaveGroupMemberResult.LAST_MEMBER, result.get());
        Mockito.verify(groupPersonageStorage).clearMemberGroup(personageId);
        Mockito.verify(groupStorage).deleteTag(groupId);
    }

    @Test
    void When_GroupHasAnotherMembers_Then_ReturnNotLastMember() {
        final var personageId = new PersonageId(1);
        final var groupId = new GroupId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.of(groupId));
        Mockito.when(groupStorage.memberCount(groupId)).thenReturn(2);

        final var result = confirmLeaveGroupMemberCommand.execute(personageId, groupId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(ConfirmLeaveGroupMemberResult.NOT_LAST_MEMBER, result.get());
        Mockito.verify(groupPersonageStorage).clearMemberGroup(personageId);
    }

}
