package ru.homyakin.seeker.game.personage.settings.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettings;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettingsStorage;

@Component
public class GetPersonageSettingsCommand {
    private final PersonageSettingsStorage storage;

    public GetPersonageSettingsCommand(PersonageSettingsStorage storage) {
        this.storage = storage;
    }

    public PersonageSettings execute(PersonageId personageId) {
        return storage.getSettings(personageId);
    }
}
