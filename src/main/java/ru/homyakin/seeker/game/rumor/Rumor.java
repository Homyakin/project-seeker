package ru.homyakin.seeker.game.rumor;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;

import java.util.List;

public record Rumor(
    // TODO delete
    int id,
    String code,
    boolean isAvailable,
    List<RumorLocale> locales
) {
    public String text(Language language) {
        return LocaleUtils.getLocaleByLanguageOrDefault(locales, language)
            .map(RumorLocale::text)
            .orElseThrow(() -> new IllegalStateException("No locale for rumor " + code));
    }

    public void validateLocale() {
        if (!LocaleUtils.checkDefaultLanguage(locales)) {
            throw new IllegalStateException("Locale must have default language " + Language.DEFAULT + " at rumor " + code);
        }
    }
}
