package ru.homyakin.seeker.locale;

import java.util.ListResourceBundle;

public abstract class AbstractResource extends ListResourceBundle {
    public static final String BASE_NAME = "ru.homyakin.seeker.locale.resource";

    public String welcome() {
        return getString(LocalizationKeys.WELCOME.value);
    }
}
