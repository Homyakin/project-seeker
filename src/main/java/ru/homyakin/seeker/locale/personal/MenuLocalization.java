package ru.homyakin.seeker.locale.personal;

import java.util.HashMap;
import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.CommonUtils;

public class MenuLocalization {
    private static final Map<Language, MenuResource> map = new HashMap<>();

    public static void add(Language language, MenuResource resource) {
        map.put(language, resource);
    }

    public static String profileButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).profileButton(), map.get(Language.DEFAULT).profileButton());
    }

    public static String languageButton(Language language) {
        return CommonUtils.ifNullThan(map.get(language).languageButton(), map.get(Language.DEFAULT).languageButton());
    }
}