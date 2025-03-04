package ru.homyakin.seeker.game.personage.settings.infra.postgres;

import ru.homyakin.seeker.game.personage.settings.entity.PersonageSetting;
import ru.homyakin.seeker.game.personage.settings.entity.PersonageSettings;

import java.util.Map;

public record JsonPersonageSettings(
    Map<PersonageSetting, Boolean> settings
) {
    public static JsonPersonageSettings from(PersonageSettings settings) {
        return new JsonPersonageSettings(settings.settings());
    }

    public PersonageSettings toDomain() {
        return new PersonageSettings(
            settings == null ? Map.of() : settings
        );
    }
}
