package ru.homyakin.seeker.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Language {
    RU("ru"),
    EN("en"),
    ;

    private final String value;

    Language(String value) {
        this.value = value;
    }

    private static final Map<String, Language> map = new HashMap<>() {{
        Arrays.stream(Language.values()).forEach(it -> put(it.value, it));
    }};
    public static final Language DEFAULT = RU;

    public static Language getOrDefault(String value) {
        if (value == null) {
            return DEFAULT;
        }
        return Optional.ofNullable(map.get(value)).orElse(DEFAULT);
    }

    public String value() {
        return value;
    }
}
