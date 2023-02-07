package ru.homyakin.seeker.locale;

public enum LocalizationKeys {
    WELCOME_GROUP("welcomeGroup"),
    WELCOME_USER("welcomeUser"),
    CHOOSE_LANGUAGE("chooseLanguage"),
    ONLY_ADMIN_ACTION("onlyAdminLanguage"),
    INTERNAL_ERROR("internalError"),
    JOIN_RAID_EVENT("joinRaidEvent"),

    RAID_STARTS_PREFIX("raidStartsPrefix"),
    HOURS_SHORT("hoursShort"),
    MINUTES_SHORT("minutesShort"),
    SUCCESS_JOIN_EVENT("successJoinEvent"),
    USER_ALREADY_IN_THIS_EVENT("userAlreadyInThisEvent"),
    USER_ALREADY_IN_OTHER_EVENT("userAlreadyInOtherEvent"),
    EXPIRED_EVENT("expiredEvent"),
    PROFILE_TEMPLATE("profileTemplate"),
    SUCCESS_RAID("successRaid"),
    FAILURE_RAID("failureRaid"),
    HELP("help"),
    CHANGE_NAME_WITHOUT_NAME("changeNameWithoutName"),
    PERSONAGE_NAME_INVALID_LENGTH("personageNameInvalidLength"),
    PERSONAGE_NAME_INVALID_SYMBOLS("personageNameInvalidSymbols"),
    SUCCESS_NAME_CHANGE("successNameChange"),
    PROFILE_LEVEL_UP("profileLevelUp"),
    NOT_ENOUGH_LEVELING_POINTS("notEnoughLevelingPoints"),
    CHOOSE_LEVEL_UP_CHARACTERISTIC("chooseLevelUpCharacteristic"),
    SUCCESS_LEVEL_UP("successLevelUp"),
    PROFILE_BUTTON("profileButton"),
    LANGUAGE_BUTTON("languageButton"),
    DUEL_MUST_BE_REPLY("duelMustBeReply"),
    DUEL_REPLY_MUST_BE_TO_USER("duelReplyMustBeToUser"),
    DUEL_WITH_YOURSELF("duelWithYourself"),
    DUEL_WITH_INITIATOR_LOW_HEALTH("duelWithInitiatorLowHealth"),
    DUEL_WITH_ACCEPTOR_LOW_HEALTH("duelWithAcceptorLowHealth"),
    PERSONAGE_ALREADY_START_DUEL("personageAlreadyStartDuel"),
    INIT_DUEL("initDuel"),
    NOT_DUEL_ACCEPTING_PERSONAGE("notDuelAcceptingPersonage"),
    EXPIRED_DUEL("expiredDuel"),
    DECLINED_DUEL("declinedDuel"),
    FINISHED_DUEL("finishedDuel"),
    ACCEPT_DUEL_BUTTON("acceptDuelButton"),
    DECLINE_DUEL_BUTTON("declineDuelButton"),
    ;

    private final String tomlKey;

    LocalizationKeys(String tomlKey) {
        this.tomlKey = tomlKey;
    }

    public String tomlKey() {
        return tomlKey;
    }
}
