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

    public String joinEvent() {
        return getString(LocalizationKeys.JOIN_EVENT.name());
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

    public String startBossEvent() {
        return getString(LocalizationKeys.START_BOSS_EVENT.name());
    }
}
