package ru.homyakin.seeker.locale;

import java.util.List;
import java.util.Optional;

public class LocaleUtils {
    public static <T extends LocaleObject> Optional<T> getLocaleByLanguageOrDefault(List<T> locales, Language language) {
        T defaultLocale = null;
        for (final var locale: locales) {
            if (locale.language() == language) {
                return Optional.of(locale);
            }
            if (locale.language() == Language.DEFAULT) {
                defaultLocale = locale;
            }
        }
        return Optional.ofNullable(defaultLocale);
    }

    public static <T extends LocaleObject> boolean checkDefaultLanguage(List<T> locales) {
        return locales.stream().anyMatch(locale -> locale.language() == Language.DEFAULT);
    }
}
