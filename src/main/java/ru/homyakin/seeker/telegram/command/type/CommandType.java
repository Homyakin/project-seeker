package ru.homyakin.seeker.telegram.command.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.locale.personal.MenuResource;

public enum CommandType {
    // TODO разделить личку, группы, коллбэки (или не надо, но хотя бы подумать)
    CHANGE_LANGUAGE("/language", CheckType.EQUALS),
    SELECT_LANGUAGE("selectLanguage", CheckType.EQUALS),
    START("/start", CheckType.EQUALS),
    JOIN_EVENT("joinEvent", CheckType.EQUALS),
    GET_PROFILE("/me", CheckType.EQUALS),
    SHOW_HELP("/help", CheckType.EQUALS),
    SELECT_HELP("help", CheckType.EQUALS),
    CHANGE_NAME("/name", CheckType.STARTS_WITH),
    LEVEL_UP("/level_up", CheckType.EQUALS),
    START_DUEL("/duel", CheckType.EQUALS),
    ACCEPT_DUEL("acceptDuel", CheckType.EQUALS),
    DECLINE_DUEL("declineDuel", CheckType.EQUALS),
    TAVERN_MENU("/menu", CheckType.EQUALS),
    ORDER("/order", CheckType.STARTS_WITH),
    RECEPTION_DESK(null, CheckType.MAP),
    BACK(null, CheckType.MAP),
    RESET_CHARACTERISTICS(null, CheckType.MAP),
    CONFIRM_RESET_CHARACTERISTICS("confirmReset", CheckType.EQUALS),
    CANCEL_RESET_CHARACTERISTICS("cancelReset", CheckType.EQUALS),
    INCREASE_CHARACTERISTIC("increaseCharacteristic", CheckType.EQUALS),
    GROUP_STATS("/stats", CheckType.EQUALS),
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
            .filter(type -> type.check(text))
            .findFirst();
    }

    public static void fillLocaleMap(MenuResource resource) {
        textToType.put(resource.profileButton(), CommandType.GET_PROFILE);
        textToType.put(resource.languageButton(), CommandType.CHANGE_LANGUAGE);
        textToType.put(resource.receptionDeskButton(), CommandType.RECEPTION_DESK);
        textToType.put(resource.backButton(), CommandType.BACK);
        textToType.put(resource.resetCharacteristicsButton(), CommandType.RESET_CHARACTERISTICS);
    }

    private boolean check(String text) {
        return switch (this.checkType) {
            case EQUALS -> this.text.equals(text);
            case STARTS_WITH -> text.startsWith(this.text);
            case MAP -> false;
        };
    }
}
