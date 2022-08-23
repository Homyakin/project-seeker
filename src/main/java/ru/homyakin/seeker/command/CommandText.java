package ru.homyakin.seeker.command;

public enum CommandText {
    CHANGE_LANGUAGE("/language"),
    SELECT_LANGUAGE("selectLanguage"),
    ;

    private final String text;

    CommandText(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
