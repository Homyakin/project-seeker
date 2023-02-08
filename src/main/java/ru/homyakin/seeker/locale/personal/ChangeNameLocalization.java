package ru.homyakin.seeker.locale.personal;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;

public class ChangeNameLocalization {
    private static final Map<Language, ChangeNameResource> map = new HashMap<>();

    public static ChangeNameResource get(Language language) {
        return map.get(language);
    }

    public static void add(Language language, ChangeNameResource resource) {
        map.put(language, resource);
    }
}
