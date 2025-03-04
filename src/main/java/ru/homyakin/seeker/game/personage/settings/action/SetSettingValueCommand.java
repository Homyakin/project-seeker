package ru.homyakin.seeker.game.personage.settings.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSetting;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettings;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettingsStorage;

@Component
public class SetSettingValueCommand {
    private final PersonageSettingsStorage storage;

    public SetSettingValueCommand(PersonageSettingsStorage storage) {
        this.storage = storage;
    }

    public PersonageSettings execute(PersonageId personageId, PersonageSetting setting, boolean value) {
        final var settings = storage
            .getSettings(personageId)
            .setValue(setting, value);
        storage.setSettings(personageId, settings);
        return settings;
    }
}
