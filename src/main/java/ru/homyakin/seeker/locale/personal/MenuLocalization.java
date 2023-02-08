package ru.homyakin.seeker.locale.personal;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;

public class MenuLocalization {
    private static final Map<Language, MenuResource> map = new HashMap<>();

    public static MenuResource get(Language language) {
        return map.get(language);
    }

    public static void add(Language language, MenuResource resource) {
        map.put(language, resource);
    }
}
