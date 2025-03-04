package ru.homyakin.seeker.game.personage.settings.entity;

import java.util.HashMap;
import java.util.Map;

public record PersonageSettings(
    Map<PersonageSetting, Boolean> settings
) {
    public PersonageSettings setValue(PersonageSetting setting, boolean value) {
        final var updated = new HashMap<>(settings);
        updated.put(setting, value);
        return new PersonageSettings(updated);
    }

    public boolean sendNotifications() {
        return getOrDefault(PersonageSetting.SEND_NOTIFICATIONS);
    }

    private boolean getOrDefault(PersonageSetting setting) {
        return settings.getOrDefault(setting, setting.defaultValue());
    }
}
