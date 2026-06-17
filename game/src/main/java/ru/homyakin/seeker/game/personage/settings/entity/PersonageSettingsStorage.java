package ru.homyakin.seeker.game.personage.settings.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;

public interface PersonageSettingsStorage {
    PersonageSettings getSettings(PersonageId personageId);

    void setSettings(PersonageId personageId, PersonageSettings settings);
}
