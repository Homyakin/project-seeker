package ru.homyakin.seeker.game.group.action;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.CheckGroupPersonage;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupProfile;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.GroupRegistrationError;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.outpost.entity.OutpostStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.test_utils.PersonageMemberGroupUtils;
import ru.homyakin.seeker.test_utils.TestRandom;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Optional;

public class GroupRegistrationCommandTest {

    private final GroupStorage groupStorage = Mockito.mock();
    private final GroupPersonageStorage groupPersonageStorage = Mockito.mock();
    private final CheckGroupPersonage checkGroupPersonage = Mockito.mock();
    private final OutpostStorage outpostStorage = Mockito.mock();
    private final GroupTagService groupTagService = new GroupTagService(
        groupStorage,
        groupPersonageStorage,
        checkGroupPersonage,
        outpostStorage
    );
    private GroupId groupId;
    private PersonageId personageId;

    @BeforeEach
    void init() {
        groupId = new GroupId(TestRandom.nextLong());
        personageId = new PersonageId(TestRandom.nextLong());
    }

    @Test
    void When_GroupIsAlreadyRegistered_Then_ReturnGroupAlreadyRegisteredError() {
        final var group = new Group(
            groupId,
            Optional.of("tag"),
            "Test Group",
            null,
            true,
            null,
            10
        );
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
    void When_PersonageNotInGroup_Then_ReturnPersonageNotGroupMemberError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.empty());

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupRegistrationError.PersonageNotGroupMember.INSTANCE, result.getLeft());
    }

    @Test
    void When_InvalidTag_Then_ReturnInvalidTagError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));

        final var result = groupTagService.register(groupId, personageId, "фыва");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupRegistrationError.InvalidTag.INSTANCE, result.getLeft());
    }

    @Test
    void When_TagExists_Then_ReturnTagAlreadyTakenError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(true);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupRegistrationError.TagAlreadyTaken.INSTANCE, result.getLeft());
    }

    @Test
    void When_RegisterPersonageNotAdmin_Then_ReturnNotAdminError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(false);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
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
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(false);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId))
            .thenReturn(true);
        Mockito.when(outpostStorage.findBuildingSlot(groupId, Building.MONOLITH))
            .thenReturn(Optional.of(new OutpostSlot.BuildingSlot(groupId, Building.MONOLITH, 1, Optional.empty(), 0)));

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(Success.INSTANCE, result.get());
        Mockito.verify(groupStorage).setTag(groupId, "TAG");
        Mockito.verify(groupPersonageStorage, Mockito.never()).setMemberGroup(personageId, groupId);
    }

    @Test
    void When_MonolithMissing_Then_ReturnMonolithLevelTooLowError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(false);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId))
            .thenReturn(true);
        Mockito.when(outpostStorage.findBuildingSlot(groupId, Building.MONOLITH))
            .thenReturn(Optional.empty());

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new GroupRegistrationError.MonolithLevelTooLow(1), result.getLeft());
    }

    @Test
    void When_MonolithLevelZero_Then_ReturnMonolithLevelTooLowError() {
        final var group = Mockito.mock(Group.class);
        Mockito.when(group.isHidden()).thenReturn(false);
        Mockito.when(group.isRegistered()).thenReturn(false);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupStorage.isTagExists("TAG")).thenReturn(false);
        Mockito.when(groupPersonageStorage.getPersonageMemberGroup(personageId))
            .thenReturn(PersonageMemberGroupUtils.withGroup(groupId));
        Mockito.when(checkGroupPersonage.isAdminInGroup(groupId, personageId))
            .thenReturn(true);
        Mockito.when(outpostStorage.findBuildingSlot(groupId, Building.MONOLITH))
            .thenReturn(Optional.of(new OutpostSlot.BuildingSlot(groupId, Building.MONOLITH, 0, Optional.empty(), 0)));

        final var result = groupTagService.register(groupId, personageId, "TAG");

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(new GroupRegistrationError.MonolithLevelTooLow(1), result.getLeft());
    }

}
