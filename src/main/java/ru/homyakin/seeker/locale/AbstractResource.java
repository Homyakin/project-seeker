package ru.homyakin.seeker.locale;

import java.util.ListResourceBundle;

public abstract class AbstractResource extends ListResourceBundle {
    public static final String BASE_NAME = "ru.homyakin.seeker.locale.resource";

    public String welcomeGroup() {
        return getString(LocalizationKeys.WELCOME_GROUP.name());
    }

    public String welcomeUser() {
        return getString(LocalizationKeys.WELCOME_USER.name());
    }

    public String chooseLanguage() {
        return getString(LocalizationKeys.CHOOSE_LANGUAGE.name());
    }

    public String onlyAdminAction() {
        return getString(LocalizationKeys.ONLY_ADMIN_ACTION.name());
    }

    public String internalError() {
        return getString(LocalizationKeys.INTERNAL_ERROR.name());
    }

    public String joinBossEvent() {
        return getString(LocalizationKeys.JOIN_BOSS_EVENT.name());
    }

    public String bossBattleStarts() {
        return getString(LocalizationKeys.BOSS_BATTLE_STARTS.name());
    }

    public String hoursShort() {
        return getString(LocalizationKeys.HOURS_SHORT.name());
    }

    public String minutesShort() {
        return getString(LocalizationKeys.MINUTES_SHORT.name());
    }

    public String successJoinEvent() {
        return getString(LocalizationKeys.SUCCESS_JOIN_EVENT.name());
    }

    public String userAlreadyInThisEvent() {
        return getString(LocalizationKeys.USER_ALREADY_IN_THIS_EVENT.name());
    }

    public String userAlreadyInOtherEvent() {
        return getString(LocalizationKeys.USER_ALREADY_IN_OTHER_EVENT.name());
    }

    public String expiredEvent() {
        return getString(LocalizationKeys.EXPIRED_EVENT.name());
    }

    public String profileTemplate() {
        return getString(LocalizationKeys.PROFILE_TEMPLATE.name());
    }

    public String successBoss() {
        return getString(LocalizationKeys.SUCCESS_BOSS.name());
    }

    public String failureBoss() {
        return getString(LocalizationKeys.FAILURE_BOSS.name());
    }

    public String topPersonagesByExpInGroup() {
        return getString(LocalizationKeys.TOP_PERSONAGES_BY_EXP_IN_GROUP.name());
    }

    public String help() {
        return getString(LocalizationKeys.HELP.name());
    }

    public String changeNameWithoutName() {
        return getString(LocalizationKeys.CHANGE_NAME_WITHOUT_NAME.name());
    }

    public String nameTooLong() {
        return getString(LocalizationKeys.NAME_TOO_LONG.name());
    }

    public String successNameChange() {
        return getString(LocalizationKeys.SUCCESS_NAME_CHANGE.name());
    }

    public String profileLevelUp() {
        return getString(LocalizationKeys.PROFILE_LEVEL_UP.name());
    }

    public String notEnoughLevelingPoints() {
        return getString(LocalizationKeys.NOT_ENOUGH_LEVELING_POINTS.name());
    }

    public String chooseLevelUpCharacteristic() {
        return getString(LocalizationKeys.CHOOSE_LEVEL_UP_CHARACTERISTIC.name());
    }

    public String successLevelUp() {
        return getString(LocalizationKeys.SUCCESS_LEVEL_UP.name());
    }
}
