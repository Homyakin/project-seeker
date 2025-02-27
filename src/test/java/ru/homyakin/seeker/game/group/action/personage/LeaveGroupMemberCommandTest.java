package ru.homyakin.seeker.game.group.action.personage;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.LeaveGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Optional;

class LeaveGroupMemberCommandTest {
    private final GroupPersonageStorage groupPersonageStorage = Mockito.mock();
    private final GroupStorage groupStorage = Mockito.mock();
    private final LeaveGroupMemberCommand leaveGroupMemberCommand = new LeaveGroupMemberCommand(
        groupPersonageStorage,
        groupStorage
    );

    @Test
    void When_PersonageNotInGroup_Then_ReturnNotGroupMemberError() {
        final var personageId = new PersonageId(1);
        final var groupId = new GroupId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.empty());

        final var result = leaveGroupMemberCommand.execute(personageId, groupId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(LeaveGroupMemberError.NotGroupMember.INSTANCE, result.getLeft());
    }

    @Test
    void When_LastMemberLeavesGroup_Then_ReturnLastMemberError() {
        final var personageId = new PersonageId(1);
        final var groupId = new GroupId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.of(groupId));
        Mockito.when(groupStorage.memberCount(groupId)).thenReturn(1);

        final var result = leaveGroupMemberCommand.execute(personageId, groupId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(LeaveGroupMemberError.LastMember.INSTANCE, result.getLeft());
    }

    @Test
    void When_PersonageIsMemberAndGroupHasMoreThanOneMember_Then_SuccessLeave() {
        final var personageId = new PersonageId(1);
        final var groupId = new GroupId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.of(groupId));
        Mockito.when(groupStorage.memberCount(groupId)).thenReturn(2);

        final var result = leaveGroupMemberCommand.execute(personageId, groupId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(Success.INSTANCE, result.get());
        Mockito.verify(groupPersonageStorage).clearMemberGroup(personageId);
    }
}
