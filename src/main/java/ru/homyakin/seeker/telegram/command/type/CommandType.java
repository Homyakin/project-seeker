package ru.homyakin.seeker.telegram.command.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.personal.MenuResource;

public enum CommandType {
    CHANGE_LANGUAGE("/language", CheckType.EQUALS),
    SELECT_LANGUAGE("selectLanguage", CheckType.EQUALS),
    START("/start", CheckType.EQUALS),
    JOIN_EVENT("joinEvent", CheckType.EQUALS),
    GET_PROFILE("/me", CheckType.EQUALS),
    HELP("/help", CheckType.EQUALS),
    CHANGE_NAME("/name", CheckType.STARTS_WITH),
    LEVEL_UP("/level_up", CheckType.EQUALS),
    UP_STRENGTH("+1" + TextConstants.STRENGTH_ICON, CheckType.EQUALS),
    UP_AGILITY("+1" + TextConstants.AGILITY_ICON, CheckType.EQUALS),
    UP_WISDOM("+1" + TextConstants.WISDOM_ICON, CheckType.EQUALS),
    START_DUEL("/duel", CheckType.EQUALS),
    ACCEPT_DUEL("acceptDuel", CheckType.EQUALS),
    DECLINE_DUEL("declineDuel", CheckType.EQUALS),
    TAVERN_MENU("/menu", CheckType.EQUALS),
    ORDER("/order", CheckType.STARTS_WITH),
    RECEPTION_DESK(null, CheckType.SKIP),
    BACK(null, CheckType.SKIP),
    RESET_STATS(null, CheckType.SKIP),
    ;

    private static final Map<String, CommandType> textToType = new HashMap<>();

    private final String text;
    private final CheckType checkType;

    CommandType(String text, CheckType checkType) {
        this.text = text;
        this.checkType = checkType;
    }

    public String getText() {
        return text;
    }

    public static Optional<CommandType> getFromString(String text) {
        if (textToType.containsKey(text)) {
            return Optional.of(textToType.get(text));
        }
        return Arrays.stream(values())
            .filter(commandText -> commandText.check(text))
            .findFirst();
    }

    public static void fillLocaleMap(MenuResource resource) {
        textToType.put(resource.profileButton(), CommandType.GET_PROFILE);
        textToType.put(resource.languageButton(), CommandType.CHANGE_LANGUAGE);
        textToType.put(resource.receptionDeskButton(), CommandType.RECEPTION_DESK);
        textToType.put(resource.backButton(), CommandType.BACK);
        textToType.put(resource.resetStatsButton(), CommandType.RESET_STATS);
    }

    private boolean check(String text) {
        return switch (this.checkType) {
            case EQUALS -> this.text.equals(text);
            case STARTS_WITH -> this.text.startsWith(text);
            case SKIP -> false;
        };
    }
}
