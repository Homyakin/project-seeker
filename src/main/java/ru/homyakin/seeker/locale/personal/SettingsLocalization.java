package ru.homyakin.seeker.locale.personal;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;

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
}
