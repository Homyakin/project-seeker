package ru.homyakin.seeker.models;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Language {
    RU(1, "ru"),
    EN(2, "en"),
    ;

    private final int id;
    private final String value;

    Language(int id, String value) {
        this.id = id;
        this.value = value;
    }

    private static final Map<Integer, Language> map = new HashMap<>() {{
        Arrays.stream(Language.values()).forEach(it -> put(it.id, it));
    }};
    public static final Language DEFAULT = RU;

    public static Language getOrDefault(Integer value) {
        if (value == null) {
            return DEFAULT;
        }
        return Optional.ofNullable(map.get(value)).orElse(DEFAULT);
    }

    public int id() {
        return id;
    }

    public String value() {
        return value;
    }
}
