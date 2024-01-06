package ru.homyakin.seeker.infrastructure.init;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.service.EventService;

public class InitGameDataTest {
    private final InitGameData initGameData = new InitGameData(
        Mockito.mock(EventService.class)
    );

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadEvents_Then_NoErrors(InitGameDataType type) {
        initGameData.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadEvents);
    }
}
