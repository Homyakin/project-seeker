package ru.homyakin.seeker.locale.personal;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.utils.StringNamedTemplate;

import java.util.Collections;

public class SettingsLocalization {
    private static final Resources<SettingsResource> resources = new Resources<>();

    public static void add(Language language, SettingsResource resource) {
        resources.add(language, resource);
    }

    public static String personageIsHidden(Language language) {
        return resources.getOrDefault(language, SettingsResource::personageIsHidden);
    }

    public static String personageIsUnhidden(Language language) {
        return resources.getOrDefault(language, SettingsResource::personageIsUnhidden);
    }

    public static String settings(Language language) {
        return resources.getOrDefault(language, SettingsResource::settings);
    }

    public static String sendNotificationsButton(Language language, boolean isEnabled) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, SettingsResource::sendNotificationsButton),
            Collections.singletonMap("enabled_icon", LocaleUtils.enabledIcon(isEnabled))
        );
    }
}
