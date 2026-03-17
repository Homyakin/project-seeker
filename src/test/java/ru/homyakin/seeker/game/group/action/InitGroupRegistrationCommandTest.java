package ru.homyakin.seeker.game.group.action;


import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.error.GroupAlreadyRegistered;
import ru.homyakin.seeker.utils.models.Success;

import java.util.Optional;

class InitGroupRegistrationCommandTest {

    private GroupStorage groupStorage;
    private InitGroupRegistrationCommand initGroupRegistrationCommand;
    private GroupId groupId;

    @BeforeEach
    void init() {
        groupStorage = Mockito.mock(GroupStorage.class);
        initGroupRegistrationCommand = new InitGroupRegistrationCommand(groupStorage);
        groupId = new GroupId(1L);
    }

    @Test
    void When_GroupIsNotRegistered_Then_ReturnSuccess() {
        final var group = new Group(groupId, Optional.empty(), "Test Group", null, true, null, 0);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));

        final var result = initGroupRegistrationCommand.execute(groupId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(Success.INSTANCE, result.get());
    }

    @Test
    void When_GroupIsAlreadyRegistered_Then_ReturnAlreadyRegisteredError() {
        final var group = new Group(groupId, Optional.of("tag"), "Test Group", null, true, null, 0);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));

        Either<GroupAlreadyRegistered, Success> result = initGroupRegistrationCommand.execute(groupId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupAlreadyRegistered.INSTANCE, result.getLeft());
    }
}
