package ru.homyakin.seeker.game.rumor;

import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record Rumor(
    String code,
    boolean isAvailable,
    Map<Language, RumorLocale> locales
) implements Localized<RumorLocale> {
    public String text(Language language) {
        return getLocaleOrDefault(language).text();
    }
}
