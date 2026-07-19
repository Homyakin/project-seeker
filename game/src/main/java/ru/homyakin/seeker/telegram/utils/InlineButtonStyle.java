package ru.homyakin.seeker.telegram.utils;

public enum InlineButtonStyle {
    PRIMARY("primary"),
    SUCCESS("success"),
    DANGER("danger"),
    ;

    private final String value;

    InlineButtonStyle(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}
