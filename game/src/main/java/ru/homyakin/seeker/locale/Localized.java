package ru.homyakin.seeker.locale;

import java.util.Map;
import java.util.Optional;

public interface Localized<T> {
    Map<Language, T> locales();

    default T getLocaleOrDefault(Language language) {
        return Optional.ofNullable(this.locales().get(language))
            .orElseGet(() -> this.locales().get(Language.DEFAULT));
    }

    default void validateLocale() {
        if (this.locales().get(Language.DEFAULT) == null) {
            throw new IllegalStateException("Locale must have default language " + Language.DEFAULT + " at " + this.toString());
        }
    }
}
