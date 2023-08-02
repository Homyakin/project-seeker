package ru.homyakin.seeker.game.rumor;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;

import java.util.List;

public record Rumor(
    int id,
    String code,
    List<RumorLocale> locales
) {
    public String text(Language language) {
        return LocaleUtils.getLocaleByLanguageOrDefault(locales, language)
            .map(RumorLocale::text)
            .orElseThrow(() -> new IllegalStateException("No locale for rumor " + code));
    }

}
