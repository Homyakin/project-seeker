package ru.homyakin.seeker.game.personage.settings.entity;

public enum PersonageSetting {
    SEND_NOTIFICATIONS(true),
    AUTO_QUESTING(false),
    ;

    private final boolean defaultValue;

    PersonageSetting(boolean defaultValue) {
        this.defaultValue = defaultValue;
    }

    public boolean defaultValue() {
        return defaultValue;
    }
}
