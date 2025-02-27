package ru.homyakin.seeker.game.group.action.personage;


import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.JoinGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.util.Optional;

class JoinGroupMemberCommandTest {
    private final GroupPersonageStorage groupPersonageStorage = Mockito.mock();
    private final GroupStorage groupStorage = Mockito.mock();
    private final JoinGroupMemberCommand joinGroupMemberCommand = new JoinGroupMemberCommand(
        groupPersonageStorage,
        groupStorage
    );

    @Test
    void When_PersonageAlreadyInGroup_Then_ReturnPersonageAlreadyInGroupError() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.of(groupId));

        final var result = joinGroupMemberCommand.execute(groupId, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(JoinGroupMemberError.PersonageAlreadyInGroup.INSTANCE, result.getLeft());
    }

    @Test
    void When_PersonageInAnotherGroup_Then_ReturnPersonageInAnotherGroupError() {
        final var groupId1 = new GroupId(1);
        final var groupId2 = new GroupId(2);
        final var personageId = new PersonageId(1);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.of(groupId2));

        final var result = joinGroupMemberCommand.execute(groupId1, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(JoinGroupMemberError.PersonageInAnotherGroup.INSTANCE, result.getLeft());
    }

    @Test
    void When_GroupNotRegistered_Then_ReturnGroupNotRegisteredError() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var group = Mockito.mock(Group.class);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.empty());

        final var result = joinGroupMemberCommand.execute(groupId, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(JoinGroupMemberError.GroupNotRegistered.INSTANCE, result.getLeft());
    }

    @Test
    void When_PersonageNotInGroupAndGroupIsRegistered_Then_JoinGroup() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var group = Mockito.mock(Group.class);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId)).thenReturn(Optional.empty());

        final var result = joinGroupMemberCommand.execute(groupId, personageId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(group, result.get());
        Mockito.verify(groupPersonageStorage).setMemberGroup(personageId, groupId);
    }
}
