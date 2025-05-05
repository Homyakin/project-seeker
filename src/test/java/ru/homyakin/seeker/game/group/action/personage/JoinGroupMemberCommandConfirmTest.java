package ru.homyakin.seeker.game.group.action.personage;


import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.CheckGroupMemberAdminError;
import ru.homyakin.seeker.game.group.error.JoinGroupMemberError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.test_utils.PersonageMemberGroupUtils;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.time.Duration;
import java.util.Optional;

class JoinGroupMemberCommandConfirmTest {
    private static final GroupConfig groupConfig = Mockito.mock();
    private final GroupPersonageStorage groupPersonageStorage = Mockito.mock();
    private final GroupStorage groupStorage = Mockito.mock();
    private final CheckGroupMemberAdminCommand checkGroupMemberAdminCommand = Mockito.mock();
    private final CheckGroupPersonage checkGroupPersonage = Mockito.mock();
    private final JoinGroupMemberCommand joinGroupMemberCommand = new JoinGroupMemberCommand(
        groupPersonageStorage,
        checkGroupMemberAdminCommand,
        checkGroupPersonage,
        groupStorage,
        groupConfig
    );

    @BeforeAll
    public static void init() {
        Mockito.when(groupConfig.personageJoinGroupTimeout()).thenReturn(joinTimeout);
    }

    @Test
    void When_PersonageAlreadyInGroup_Then_ReturnPersonageAlreadyInGroupError() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var confirming = new PersonageId(2);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));

        final var result = joinGroupMemberCommand.confirm(groupId, confirming, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(JoinGroupMemberError.PersonageAlreadyInGroup.INSTANCE, result.getLeft());
    }

    @Test
    void When_PersonageInAnotherGroup_Then_ReturnPersonageInAnotherGroupError() {
        final var groupId1 = new GroupId(1);
        final var groupId2 = new GroupId(2);
        final var personageId = new PersonageId(1);
        final var confirming = new PersonageId(2);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId2));

        final var result = joinGroupMemberCommand.confirm(groupId1, confirming, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(JoinGroupMemberError.PersonageInAnotherGroup.INSTANCE, result.getLeft());
    }

    @Test
    void When_GroupNotRegistered_Then_ReturnGroupNotRegisteredError() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var group = Mockito.mock(Group.class);
        final var confirming = new PersonageId(2);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());

        final var result = joinGroupMemberCommand.confirm(groupId, confirming, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(JoinGroupMemberError.GroupNotRegistered.INSTANCE, result.getLeft());
    }

    @Test
    void When_PersonageNotInGroupAndGroupIsRegisteredAndAdmin_Then_JoinGroup() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var group = Mockito.mock(Group.class);
        final var confirming = new PersonageId(2);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId)).thenReturn(true);

        final var result = joinGroupMemberCommand.confirm(groupId, confirming, personageId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(Success.INSTANCE, result.get());
        Mockito.verify(groupPersonageStorage).setMemberGroup(personageId, groupId);
    }

    @Test
    void Given_ConfirmingAdminMember_When_PersonageNotInGroupAndGroupIsRegisteredAndNotAdmin_Then_Success() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var group = Mockito.mock(Group.class);
        final var confirming = new PersonageId(2);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId)).thenReturn(false);
        Mockito.when(checkGroupMemberAdminCommand.execute(groupId, confirming))
            .thenReturn(Either.right(Success.INSTANCE));

        final var result = joinGroupMemberCommand.confirm(groupId, confirming, personageId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(Success.INSTANCE, result.get());
        Mockito.verify(groupPersonageStorage).setMemberGroup(personageId, groupId);
    }

    @Test
    void Given_ConfirmingNotAdminMember_When_PersonageNotInGroupAndGroupIsRegisteredAndNotAdmin_Then_Error() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var group = Mockito.mock(Group.class);
        final var confirming = new PersonageId(2);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId)).thenReturn(false);
        Mockito.when(checkGroupMemberAdminCommand.execute(groupId, confirming))
            .thenReturn(Either.left(CheckGroupMemberAdminError.NotAnAdmin.INSTANCE));

        final var result = joinGroupMemberCommand.confirm(groupId, confirming, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(CheckGroupMemberAdminError.NotAnAdmin.INSTANCE, result.getLeft());
        Mockito.verify(groupPersonageStorage, Mockito.times(0))
            .setMemberGroup(personageId, groupId);
    }

    @Test
    void Given_ConfirmingAdminNotMember_When_PersonageNotInGroupAndGroupIsRegisteredAndNotAdmin_Then_Error() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var group = Mockito.mock(Group.class);
        final var confirming = new PersonageId(2);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId)).thenReturn(false);
        Mockito.when(checkGroupMemberAdminCommand.execute(groupId, confirming))
            .thenReturn(Either.left(CheckGroupMemberAdminError.PersonageNotInGroup.INSTANCE));

        final var result = joinGroupMemberCommand.confirm(groupId, confirming, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(CheckGroupMemberAdminError.PersonageNotInGroup.INSTANCE, result.getLeft());
        Mockito.verify(groupPersonageStorage, Mockito.times(0))
            .setMemberGroup(personageId, groupId);
    }

    @Test
    void When_PersonageHasJoinTimeout_Then_ReturnTimeoutError() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var group = Mockito.mock(Group.class);
        final var confirming = new PersonageId(2);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withDate(TimeUtils.moscowTime()));

        final var result = joinGroupMemberCommand.confirm(groupId, confirming, personageId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertInstanceOf(JoinGroupMemberError.PersonageJoinTimeout.class, result.getLeft());
    }

    @Test
    void When_PersonageHasExpiredJoinTimeout_Then_ReturnJoinTimeoutError() {
        final var groupId = new GroupId(1);
        final var personageId = new PersonageId(1);
        final var group = Mockito.mock(Group.class);
        final var confirming = new PersonageId(2);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withDate(TimeUtils.moscowTime().minus(joinTimeout)));
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId)).thenReturn(true);

        final var result = joinGroupMemberCommand.confirm(groupId, confirming, personageId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(Success.INSTANCE, result.get());
        Mockito.verify(groupPersonageStorage).setMemberGroup(personageId, groupId);
    }

    private static final Duration joinTimeout = Duration.ofMinutes(1);
}
