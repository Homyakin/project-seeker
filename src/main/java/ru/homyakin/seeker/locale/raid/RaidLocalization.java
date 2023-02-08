package ru.homyakin.seeker.locale.raid;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;

public class RaidLocalization {
    private static final Map<Language, RaidResource> map = new HashMap<>();

    public static RaidResource get(Language language) {
        return map.get(language);
    }

    public static void add(Language language, RaidResource resource) {
        map.put(language, resource);
    }
}
