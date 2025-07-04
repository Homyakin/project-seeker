package ru.homyakin.seeker.locale;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public enum Language {
    RU(1, "ru"),
    EN(2, "en"),
    KZ(3, "kz"),
    ES(4, "es"),
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
    public static final Language DEFAULT = RU; // считаем, что данные язык есть во всех локализациях

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

    public String buttonText() {
        final var coverage = "%.2f%%".formatted(LocalizationCoverage.coverage(this) * 100);
        return value + "(" + coverage + ")";
    }
}
