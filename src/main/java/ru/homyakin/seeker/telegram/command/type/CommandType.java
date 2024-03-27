package ru.homyakin.seeker.telegram.command.type;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import ru.homyakin.seeker.locale.personal.MenuResource;
import ru.homyakin.seeker.utils.CommonUtils;

public enum CommandType {
    // TODO разделить личку, группы, коллбэки (или не надо, но хотя бы подумать)
    CHANGE_LANGUAGE("/language", CheckType.EQUALS),
    SELECT_LANGUAGE("selectLanguage", CheckType.EQUALS),
    START("/start", CheckType.EQUALS),
    JOIN_EVENT("joinEvent", CheckType.EQUALS),
    GET_PROFILE("/me", CheckType.EQUALS),
    SHOW_HELP("/help", CheckType.EQUALS),
    SELECT_HELP("help", CheckType.EQUALS),
    INIT_CHANGE_NAME(null, CheckType.MAP),
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
    SPIN("/work", CheckType.EQUALS),
    SPIN_TOP("/top_work_group", CheckType.EQUALS),
    SET_ACTIVE_TIME("/set_active_time", CheckType.STARTS_WITH),
    GET_ACTIVE_TIME("/get_active_time", CheckType.EQUALS),
    CONSUME_MENU_ITEM_ORDER("consume", CheckType.EQUALS),
    PERSONAGE_STATS("/stats_me", CheckType.EQUALS),
    RAID_REPORT("/report_raid", CheckType.EQUALS),
    SHOW_BADGES(null, CheckType.MAP),
    SELECT_BADGE("selectBadge", CheckType.EQUALS),
    TOP_RAID_WEEK("/top_raid_week", CheckType.EQUALS),
    TOP_RAID_WEEK_GROUP("/top_raid_week_group", CheckType.EQUALS),
    TOP("/top", CheckType.EQUALS),
    // RANDOM_ITEM("/gen", CheckType.EQUALS), // Для тестов
    INVENTORY(null, CheckType.MAP),
    PUT_ON("/on", CheckType.STARTS_WITH),
    TAKE_OFF("/off", CheckType.STARTS_WITH),
    DROP_ITEM("/drop", CheckType.STARTS_WITH),
    CONFIRM_DROP_ITEM("confirmDropItem", CheckType.STARTS_WITH),
    REJECT_DROP_ITEM("rejectDropItem", CheckType.EQUALS),
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
        CommonUtils.putIfKeyPresents(textToType, resource.profileButton(), CommandType.GET_PROFILE);
        CommonUtils.putIfKeyPresents(textToType, resource.languageButton(), CommandType.CHANGE_LANGUAGE);
        CommonUtils.putIfKeyPresents(textToType, resource.receptionDeskButton(), CommandType.RECEPTION_DESK);
        CommonUtils.putIfKeyPresents(textToType, resource.backButton(), CommandType.BACK);
        CommonUtils.putIfKeyPresents(textToType, resource.resetCharacteristicsButton(), CommandType.RESET_CHARACTERISTICS);
        CommonUtils.putIfKeyPresents(textToType, resource.changeNameButton(), CommandType.INIT_CHANGE_NAME);
        CommonUtils.putIfKeyPresents(textToType, resource.showBadgesButton(), CommandType.SHOW_BADGES);
        CommonUtils.putIfKeyPresents(textToType, resource.inventoryButton(), CommandType.INVENTORY);
    }

    private boolean check(String text) {
        return switch (this.checkType) {
            case EQUALS -> this.text.equals(text);
            case STARTS_WITH -> text.startsWith(this.text);
            case MAP -> false;
        };
    }
}
