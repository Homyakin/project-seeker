package ru.homyakin.seeker.locale;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public enum WordForm {
    MASCULINE,
    FEMININE,
    NEUTER,
    PLURAL,
    WITHOUT,
    ;

    private static final Map<Language, Set<WordForm>> languageRequiredForms = new HashMap<>() {{
       put(Language.RU, Set.of(MASCULINE, FEMININE, NEUTER, PLURAL));
    }};

    public static Optional<Set<WordForm>> languageRequiredForms(Language language) {
        return Optional.ofNullable(languageRequiredForms.get(language));
    }
}
