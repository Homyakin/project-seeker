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
    START("/start", CheckType.STARTS_WITH),
    JOIN_EVENT("joinEvent", CheckType.EQUALS),
    JOIN_RAID("joinRaid", CheckType.EQUALS),
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
    WORKER_OF_DAY("/work", CheckType.EQUALS),
    WORKER_OF_DAY_TOP("/top_work_group", CheckType.EQUALS),
    CONSUME_MENU_ITEM_ORDER("consume", CheckType.EQUALS),
    PERSONAGE_STATS("/stats_me", CheckType.EQUALS),
    RAID_REPORT("/report_raid", CheckType.EQUALS),
    SHOW_BADGES(null, CheckType.MAP),
    SELECT_BADGE("selectBadge", CheckType.EQUALS),
    TOP_RAID_WEEK("/top_raid_week", CheckType.EQUALS),
    TOP_RAID_WEEK_GROUP("/top_raid_week_group", CheckType.EQUALS),
    TOP_POWER_GROUP("/top_power_group", CheckType.EQUALS),
    TOP_DONATE("/top_donate", CheckType.EQUALS),
    TOP_TAVERN_SPENT("/top_tavern_spent", CheckType.EQUALS),
    TOP("/top", CheckType.EQUALS),
    INVENTORY(null, CheckType.MAP),
    OPEN_SHOP(null, CheckType.MAP),
    BULLETIN_BOARD(null, CheckType.MAP),
    TAKE_PERSONAL_QUEST(null, CheckType.MAP),
    TAKE_PERSONAL_QUEST_COMMAND("/take_quest", CheckType.STARTS_WITH),
    SELL_ITEM("/sell", CheckType.STARTS_WITH),
    BUY_ITEM("/buy", CheckType.STARTS_WITH),
    PUT_ON("/on", CheckType.STARTS_WITH),
    TAKE_OFF("/off", CheckType.STARTS_WITH),
    CANCEL_EVENT("/ecancel", CheckType.STARTS_WITH),
    SETTINGS("/settings", CheckType.EQUALS),
    TOGGLE_EVENT_INTERVAL("toggleEventInterval", CheckType.STARTS_WITH),
    SET_TIME_ZONE("/set_time_zone", CheckType.STARTS_WITH),
    TOGGLE_HIDE_PERSONAGE("/hide_me", CheckType.EQUALS),
    TOGGLE_HIDE_GROUP("/hide", CheckType.EQUALS),
    INIT_FEEDBACK("/feedback", CheckType.EQUALS),
    THROW_ORDER("/throw", CheckType.EQUALS),
    CHANGE_GROUP_NAME("/name", CheckType.EQUALS),
    TOP_GROUP_RAID_WEEK("/topg_raid_week", CheckType.EQUALS),
    TOP_GROUP_RAID_LEVEL("/topg_raid_level", CheckType.EQUALS),
    GROUP_INFO("/group_info", CheckType.EQUALS),
    GROUP_MEMBERS("/group_members", CheckType.EQUALS),
    REGISTER_GROUP("/register", CheckType.STARTS_WITH),
    DONATE_MONEY("/donate", CheckType.STARTS_WITH),
    GIVE_MONEY("/give", CheckType.STARTS_WITH),
    JOIN_GROUP("/group_join", CheckType.EQUALS),
    LEAVE_GROUP("/group_leave", CheckType.EQUALS),
    LEAVE_GROUP_CONFIRM("confirmLeaveGroup", CheckType.STARTS_WITH),
    LEAVE_GROUP_CANCEL("cancelLeaveGroup", CheckType.STARTS_WITH),
    GROUP_COMMANDS("/group_commands", CheckType.EQUALS),
    TOGGLE_PERSONAGE_SETTING("tglPersStng", CheckType.STARTS_WITH),
    SHOW_WORLD_RAID_INFO(null, CheckType.MAP),
    WORLD_RAID_DONATE("/wr_donate", CheckType.EQUALS),
    WORLD_RAID_RESEARCH_TOP("/top_wr_research", CheckType.EQUALS),
    JOIN_WORLD_RAID("joinWorldRaid", CheckType.EQUALS),
    WORLD_RAID_REPORT("/wr_report", CheckType.EQUALS),
    CONFIRM_GROUP_JOIN("confirmGroupJoin", CheckType.EQUALS),
    CANCEL_GROUP_JOIN("cancelGroupJoin", CheckType.EQUALS),
    CHANGE_TAG("/change_tag", CheckType.STARTS_WITH),
    ENHANCE_TABLE(null, CheckType.MAP),
    ENHANCE_INFO("/enhance", CheckType.STARTS_WITH),
    ADD_MODIFIER("/addmod", CheckType.STARTS_WITH),
    REPAIR("/repair", CheckType.STARTS_WITH),
    THROW_ORDER_TO_GROUP("/gthrow", CheckType.EQUALS),
    SHOW_BADGES_GROUP("/badges", CheckType.EQUALS),
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
        CommonUtils.putIfKeyPresents(textToType, resource.shopButton(), CommandType.OPEN_SHOP);
        CommonUtils.putIfKeyPresents(textToType, resource.bulletinBoardButton(), CommandType.BULLETIN_BOARD);
        CommonUtils.putIfKeyPresents(textToType, resource.takePersonalQuestButton(), CommandType.TAKE_PERSONAL_QUEST);
        CommonUtils.putIfKeyPresents(textToType, resource.worldRaidButton(), CommandType.SHOW_WORLD_RAID_INFO);
        CommonUtils.putIfKeyPresents(textToType, resource.enhanceButton(), CommandType.ENHANCE_TABLE);
    }

    private boolean check(String text) {
        return switch (this.checkType) {
            case EQUALS -> this.text.equals(text);
            case STARTS_WITH -> text.startsWith(this.text);
            case MAP -> false;
        };
    }
}
