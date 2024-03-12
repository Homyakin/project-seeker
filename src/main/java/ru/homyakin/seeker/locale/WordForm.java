package ru.homyakin.seeker.locale;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum WordForm {
    MASCULINE,
    FEMININE,
    NEUTER,
    PLURAL,
    WITHOUT,
    ;

    private static final Map<Language, List<WordForm>> languageRequiredForms = new HashMap<>() {{
       put(Language.RU, List.of(MASCULINE, FEMININE, NEUTER, PLURAL));
    }};
}
