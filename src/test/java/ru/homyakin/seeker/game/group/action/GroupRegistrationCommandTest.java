package ru.homyakin.seeker.game.group.action;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.CheckGroupPersonage;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupProfile;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.GroupRegistrationError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.test_utils.PersonageMemberGroupUtils;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Optional;

public class GroupRegistrationCommandTest {

    private final GroupStorage groupStorage = Mockito.mock();
    private final GroupPersonageStorage groupPersonageStorage = Mockito.mock();
    private final GroupConfig groupConfig = Mockito.mock();
    private final CheckGroupPersonage checkGroupPersonage = Mockito.mock();
    private final GroupTagService groupTagService = new GroupTagService(
        groupStorage,
        groupPersonageStorage,
        checkGroupPersonage,
        groupConfig
    );
    private GroupId groupId;
    private PersonageId personageId;

    @BeforeEach
    void init() {
        groupId = new GroupId(TestRandom.nextLong());
        personageId = new PersonageId(TestRandom.nextLong());
        Mockito.when(groupConfig.registrationPrice()).thenReturn(new Money(1000));
    }

    @Test
    void When_GroupIsAlreadyRegistered_Then_ReturnGroupAlreadyRegisteredError() {
        final var group = new Group(groupId, Optional.of("tag"), "Test Group", null, true, null);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupRegistrationError.GroupAlreadyRegistered.INSTANCE, result.getLeft());
    }

    @Test
    void When_GroupIsHidden_Then_ReturnHiddenGroupError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(true);
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupRegistrationError.HiddenGroup.INSTANCE, result.getLeft());
    }

    @Test
    void When_PersonageIsInAnotherGroup_Then_ReturnPersonageInAnotherGroupError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(new GroupId(TestRandom.nextLong())));

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupRegistrationError.PersonageInAnotherGroup.INSTANCE, result.getLeft());
    }

    @Test
    void When_InsufficientFunds_Then_ReturnNotEnoughMoneyError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        final var profile = groupProfile(500);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new GroupRegistrationError.NotEnoughMoney(new Money(1000)), result.getLeft());
    }

    @Test
    void When_InvalidTag_Then_ReturnInvalidTagError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        final var profile = groupProfile(1000);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());

        final var result = groupTagService.register(groupId, personageId, "фыва");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupRegistrationError.InvalidTag.INSTANCE, result.getLeft());
    }

    @Test
    void When_TagExists_Then_ReturnTagAlreadyTakenError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        final var profile = groupProfile(1000);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(true);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupRegistrationError.TagAlreadyTaken.INSTANCE, result.getLeft());
    }

    @Test
    void When_RegisterPersonageNotAdmin_Then_ReturnNotAdminError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        final var profile = groupProfile(1000);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(false);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId))
            .thenReturn(false);

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupRegistrationError.NotAdmin.INSTANCE, result.getLeft());
    }

    @Test
    void When_SuccessfulRegistration_Then_ReturnSuccess() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        final var profile = groupProfile(1000);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(false);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId))
            .thenReturn(true);

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(Success.INSTANCE, result.get());
        Mockito.verify(groupStorage).setTagAndTakeMoney(groupId, "TAG", new Money(1000));
        Mockito.verify(groupPersonageStorage).setMemberGroup(personageId, groupId);
    }

    private GroupProfile groupProfile(int moneyValue) {
        return new GroupProfile(
            groupId,
            "Test Group",
            Optional.empty(),
            null,
            new Money(moneyValue),
            1
        );
    }
}
