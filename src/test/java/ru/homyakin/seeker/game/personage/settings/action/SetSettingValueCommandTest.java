package ru.homyakin.seeker.game.personage.settings.action;


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSetting;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettings;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettingsStorage;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SetSettingValueCommandTest {

    private final PersonageSettingsStorage storage = mock();
    private final SetSettingValueCommand command = new SetSettingValueCommand(storage);
    private final PersonageId personageId = new PersonageId(1L);

    @Test
    void When_SetExistingSettingToNewValue_Then_UpdateValue() {
        final var initialSettings = new PersonageSettings(
            Map.of(PersonageSetting.SEND_NOTIFICATIONS, false)
        );
        when(storage.getSettings(personageId)).thenReturn(initialSettings);

        final var updatedSettings = command.execute(personageId, PersonageSetting.SEND_NOTIFICATIONS, true);

        final var expected = new PersonageSettings(Map.of(PersonageSetting.SEND_NOTIFICATIONS, true));
        Assertions.assertEquals(expected, updatedSettings);
        verify(storage).setSettings(personageId, expected);

    }

    @Test
    void When_SetExistingSettingToSameValue_Then_NotChangeValue() {
        final var initialSettings = new PersonageSettings(
            Map.of(PersonageSetting.SEND_NOTIFICATIONS, false)
        );
        when(storage.getSettings(personageId)).thenReturn(initialSettings);

        final var updatedSettings = command.execute(personageId, PersonageSetting.SEND_NOTIFICATIONS, false);

        final var expected = new PersonageSettings(Map.of(PersonageSetting.SEND_NOTIFICATIONS, false));
        Assertions.assertEquals(expected, updatedSettings);
    }

    @Test
    void Given_TwoSettings_When_UpdateOneSettings_Then_UpdateOnlyThatSetting() {
        final var settingsMap = new HashMap<PersonageSetting, Boolean>();
        settingsMap.put(PersonageSetting.SEND_NOTIFICATIONS, false);
        settingsMap.put(PersonageSetting.AUTO_QUESTING, false);
        when(storage.getSettings(personageId)).thenReturn(new PersonageSettings(settingsMap));

        PersonageSettings updatedSettings = command.execute(personageId, PersonageSetting.SEND_NOTIFICATIONS, true);

        Assertions.assertTrue(updatedSettings.settings().get(PersonageSetting.SEND_NOTIFICATIONS));
        Assertions.assertFalse(updatedSettings.settings().get(PersonageSetting.AUTO_QUESTING));
    }

}
