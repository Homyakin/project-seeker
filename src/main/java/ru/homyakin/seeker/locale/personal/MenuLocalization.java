package ru.homyakin.seeker.locale.personal;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Resources;

public class MenuLocalization {
    private static final Resources<MenuResource> resources = new Resources<>();

    public static void add(Language language, MenuResource resource) {
        resources.add(language, resource);
    }

    public static String profileButton(Language language) {
        return resources.getOrDefault(language, MenuResource::profileButton);
    }

    public static String languageButton(Language language) {
        return resources.getOrDefault(language, MenuResource::languageButton);
    }

    public static String receptionDeskButton(Language language) {
        return resources.getOrDefault(language, MenuResource::receptionDeskButton);
    }

    public static String backButton(Language language) {
        return resources.getOrDefault(language, MenuResource::backButton);
    }

    public static String changeNameButton(Language language) {
        return resources.getOrDefault(language, MenuResource::changeNameButton);
    }

    public static String showBadgesButton(Language language) {
        return resources.getOrDefault(language, MenuResource::showBadgesButton);
    }

    public static String resetStatsButton(Language language) {
        return resources.getOrDefault(language, MenuResource::resetCharacteristicsButton);
    }

    public static String inventoryButton(Language language) {
        return resources.getOrDefault(language, MenuResource::inventoryButton);
    }

    public static String shopButton(Language language) {
        return resources.getOrDefault(language, MenuResource::shopButton);
    }

    public static String bulletinBoardButton(Language language) {
        return resources.getOrDefault(language, MenuResource::bulletinBoardButton);
    }

    public static String takePersonalQuestButton(Language language) {
        return resources.getOrDefault(language, MenuResource::takePersonalQuestButton);
    }

    public static String worldRaidButton(Language language) {
        return resources.getOrDefault(language, MenuResource::worldRaidButton);
    }

    public static String enhanceButton(Language language) {
        return resources.getOrDefault(language, MenuResource::enhanceButton);
    }
}
