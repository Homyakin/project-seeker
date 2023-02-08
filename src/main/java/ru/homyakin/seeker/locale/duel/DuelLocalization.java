package ru.homyakin.seeker.locale.duel;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;

public class DuelLocalization {
    private static final Map<Language, DuelResource> map = new HashMap<>();

    public static DuelResource get(Language language) {
        return map.get(language);
    }

    public static void add(Language language, DuelResource resource) {
        map.put(language, resource);
    }
}
