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
import ru.homyakin.seeker.game.group.error.ChangeTagError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.test_utils.PersonageMemberGroupUtils;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Optional;

public class GroupTagServiceChangeTagTest {

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
        Mockito.when(groupConfig.changeTagPrice()).thenReturn(new Money(300));
    }

    @Test
    void When_GroupNotRegistered_Then_ReturnGroupNotRegisteredError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));

        final var result = groupTagService.changeTag(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(ChangeTagError.GroupNotRegistered.INSTANCE, result.getLeft());
    }

    @Test
    void When_PersonageNotInGroup_Then_ReturnPersonageNotInGroupError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());

        final var result = groupTagService.changeTag(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(ChangeTagError.PersonageNotInGroup.INSTANCE, result.getLeft());
    }

    @Test
    void When_ChangeTagNotEnoughMoney_Then_ReturnNotEnoughMoneyError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        final var profile = groupProfile(200);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));

        final var result = groupTagService.changeTag(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new ChangeTagError.NotEnoughMoney(new Money(300)), result.getLeft());
    }

    @Test
    void When_ChangeTagInvalidTag_Then_ReturnInvalidTagError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        final var profile = groupProfile(1000);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));

        final var result = groupTagService.changeTag(groupId, personageId, "фыва");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(ChangeTagError.InvalidTag.INSTANCE, result.getLeft());
    }

    @Test
    void When_ChangeTagTagAlreadyTaken_Then_ReturnTagAlreadyTakenError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        final var profile = groupProfile(1000);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(true);

        final var result = groupTagService.changeTag(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(ChangeTagError.TagAlreadyTaken.INSTANCE, result.getLeft());
    }

    @Test
    void When_ChangeTagNotAdmin_Then_ReturnNotAdminError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        final var profile = groupProfile(300);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(false);
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId)).thenReturn(false);

        final var result = groupTagService.changeTag(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(ChangeTagError.NotAdmin.INSTANCE, result.getLeft());
    }

    @Test
    void When_ChangeTagSuccess_Then_ReturnSuccess() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isRegistered()).thenReturn(true);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        final var profile = groupProfile(300);
        Mockito.when(groupStorage.getProfile(groupId)).thenReturn(Optional.of(profile));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(false);
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId)).thenReturn(true);

        final var result = groupTagService.changeTag(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(Success.INSTANCE, result.get());
        Mockito.verify(groupStorage).setTagAndTakeMoney(groupId, "TAG", new Money(300));
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
