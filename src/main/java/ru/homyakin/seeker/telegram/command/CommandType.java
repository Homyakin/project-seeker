package ru.homyakin.seeker.telegram.command;

import java.util.Arrays;
import java.util.Optional;
import ru.homyakin.seeker.infrastructure.TextConstants;

public enum CommandType {
    CHANGE_LANGUAGE("/language"),
    SELECT_LANGUAGE("selectLanguage"),
    START("/start"),
    JOIN_EVENT("joinEvent"),
    GET_PROFILE("/me"),
    TOP("/top"),
    HELP("/help"),
    CHANGE_NAME("/name"),
    LEVEL_UP("/level_up"),
    UP_STRENGTH("+1" + TextConstants.STRENGTH_ICON),
    UP_AGILITY("+1" + TextConstants.AGILITY_ICON),
    UP_WISDOM("+1" + TextConstants.WISDOM_ICON),
    ;

    public static final String CALLBACK_DELIMITER = "~";

    private final String text;

    CommandType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static Optional<CommandType> getFromString(String text) {
        return Arrays.stream(values()).filter(commandText -> commandText.text.equals(text)).findFirst();
    }
}
