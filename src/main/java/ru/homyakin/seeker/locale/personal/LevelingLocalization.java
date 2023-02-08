package ru.homyakin.seeker.locale.personal;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;

public class LevelingLocalization {
    private static final Map<Language, LevelingResource> map = new HashMap<>();

    public static LevelingResource get(Language language) {
        return map.get(language);
    }

    public static void add(Language language, LevelingResource resource) {
        map.put(language, resource);
    }
}
