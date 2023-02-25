package ru.homyakin.seeker.telegram.command;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.personal.MenuResource;

public enum CommandType {
    CHANGE_LANGUAGE("/language"),
    SELECT_LANGUAGE("selectLanguage"),
    START("/start"),
    JOIN_EVENT("joinEvent"),
    GET_PROFILE("/me"),
    HELP("/help"),
    CHANGE_NAME("/name"),
    LEVEL_UP("/level_up"),
    UP_STRENGTH("+1" + TextConstants.STRENGTH_ICON),
    UP_AGILITY("+1" + TextConstants.AGILITY_ICON),
    UP_WISDOM("+1" + TextConstants.WISDOM_ICON),
    START_DUEL("/duel"),
    ACCEPT_DUEL("acceptDuel"),
    DECLINE_DUEL("declineDuel"),
    TAVERN_MENU("/menu"),
    ORDER("/order"),
    ;

    public static final String CALLBACK_DELIMITER = "~";

    // TODO переделать все команды на мапу
    private static final Map<String, CommandType> textToType = new HashMap<>();

    private final String text;

    CommandType(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public static Optional<CommandType> getFromString(String text) {
        if (textToType.containsKey(text)) {
            return Optional.of(textToType.get(text));
        }
        final var result = Arrays.stream(values())
            .filter(commandText -> commandText.text.equals(text))
            .findFirst();
        if (result.isPresent()) {
            return result;
        }
        //TODO подумать как лучше сделать
        if (text.startsWith(ORDER.text)) {
            return Optional.of(ORDER);
        }
        return Optional.empty();
    }

    public static void fillLocaleMap(MenuResource resource) {
        textToType.put(resource.profileButton(), CommandType.GET_PROFILE);
        textToType.put(resource.languageButton(), CommandType.CHANGE_LANGUAGE);
    }
}
