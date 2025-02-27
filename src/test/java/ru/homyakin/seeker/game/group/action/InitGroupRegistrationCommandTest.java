package ru.homyakin.seeker.game.group.action;


import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.error.GroupAlreadyRegistered;
import ru.homyakin.seeker.game.models.Money;

import java.util.Optional;

class InitGroupRegistrationCommandTest {

    private GroupStorage groupStorage;
    private GroupConfig groupConfig;
    private InitGroupRegistrationCommand initGroupRegistrationCommand;
    private GroupId groupId;

    @BeforeEach
    void init() {
        groupStorage = Mockito.mock(GroupStorage.class);
        groupConfig = Mockito.mock(GroupConfig.class);
        initGroupRegistrationCommand = new InitGroupRegistrationCommand(groupStorage, groupConfig);
        groupId = new GroupId(1L);
    }

    @Test
    void When_GroupIsNotRegistered_Then_Return1000Money() {
        final var group = new Group(groupId, Optional.empty(), "Test Group", true, null);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));
        Mockito.when(groupConfig.registrationPrice()).thenReturn(new Money(1000));

        final var result = initGroupRegistrationCommand.execute(groupId);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(new Money(1000), result.get());
    }

    @Test
    void When_GroupIsAlreadyRegistered_Then_ReturnAlreadyRegisteredError() {
        final var group = new Group(groupId, Optional.of("tag"), "Test Group", true, null);
        Mockito.when(groupStorage.get(groupId)).thenReturn(Optional.of(group));

        Either<GroupAlreadyRegistered, Money> result = initGroupRegistrationCommand.execute(groupId);

        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(GroupAlreadyRegistered.INSTANCE, result.getLeft());
    }
}
