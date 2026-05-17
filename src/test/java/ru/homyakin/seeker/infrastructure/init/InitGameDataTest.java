package ru.homyakin.seeker.infrastructure.init;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.item.ItemCatalogService;
import ru.homyakin.seeker.game.item.LegacyItemService;
import ru.homyakin.seeker.game.item.modifier.LegacyItemModifierService;
import ru.homyakin.seeker.game.badge.action.BadgeService;
import ru.homyakin.seeker.game.rumor.RumorService;
import ru.homyakin.seeker.game.tavern_menu.menu.MenuService;

import static org.mockito.Mockito.mock;

public class InitGameDataTest {
    private final InitGameDataConfig config = new InitGameDataConfig();
    private final InitGameData initGameData = new InitGameData(
        mock(EventService.class),
        mock(MenuService.class),
        mock(RumorService.class),
        mock(BadgeService.class),
        mock(LegacyItemService.class),
        mock(LegacyItemModifierService.class),
        mock(ItemCatalogService.class),
        config
    );

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadRaids_Then_NoErrors(InitGameDataType type) {
        config.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadRaids);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadMenuItems_Then_NoErrors(InitGameDataType type) {
        config.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadMenuItems);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadRumors_Then_NoErrors(InitGameDataType type) {
        config.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadRumors);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadBadges_Then_NoErrors(InitGameDataType type) {
        config.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadBadges);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadItems_Then_NoErrors(InitGameDataType type) {
        config.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadItems);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadPersonalQuests_Then_NoErrors(InitGameDataType type) {
        config.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadPersonalQuests);
    }

    @ParameterizedTest
    @EnumSource(InitGameDataType.class)
    public void When_LoadWorldRaids_Then_NoErrors(InitGameDataType type) {
        config.setType(type);
        Assertions.assertDoesNotThrow(initGameData::loadWorldRaids);
    }
}
