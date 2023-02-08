package ru.homyakin.seeker.locale.common;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;

public class CommonLocalization {
    private static final Map<Language, CommonResource> map = new HashMap<>();

    public static CommonResource get(Language language) {
        return map.get(language);
    }

    public static void add(Language language, CommonResource resource) {
        map.put(language, resource);
    }
}
