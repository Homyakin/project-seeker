package ru.homyakin.seeker.infrastructure.init;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.personage.badge.BadgeService;
import ru.homyakin.seeker.game.rumor.RumorService;
import ru.homyakin.seeker.game.tavern_menu.MenuService;

public class InitGameDataTest {
    private final InitGameData initGameData = new InitGameData(
        Mockito.mock(EventService.class),
        Mockito.mock(MenuService.class),
        Mockito.mock(RumorService.class),
        Mockito.mock(BadgeService.class),
        Mockito.mock(ItemService.class)
    );

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadEvents_Then_NoErrors(InitGameDataType type) {
        initGameData.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadEvents);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadMenuItems_Then_NoErrors(InitGameDataType type) {
        initGameData.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadMenuItems);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadRumors_Then_NoErrors(InitGameDataType type) {
        initGameData.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadRumors);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadBadges_Then_NoErrors(InitGameDataType type) {
        initGameData.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadBadges);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadItems_Then_NoErrors(InitGameDataType type) {
        initGameData.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadItems);
    }
}
