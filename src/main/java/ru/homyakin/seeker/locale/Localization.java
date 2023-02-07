package ru.homyakin.seeker.locale;

import java.util.HashMap;
import java.util.Map;

public class Localization {
    private static final Map<Language, CommonResource> map = new HashMap<>();

    public static CommonResource get(Language language) {
        return map.get(language);
    }

    public static void add(Language language, CommonResource resource) {
        map.put(language, resource);
    }
}
