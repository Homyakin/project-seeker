package ru.homyakin.seeker.locale;

import java.util.ListResourceBundle;

public abstract class AbstractResource extends ListResourceBundle {
    public static final String BASE_NAME = "ru.homyakin.seeker.locale.resource";

    public String welcome() {
        return getString(LocalizationKeys.WELCOME_GROUP.name());
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
}
