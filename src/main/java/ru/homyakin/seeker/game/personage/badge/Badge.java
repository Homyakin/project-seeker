package ru.homyakin.seeker.game.personage.badge;

import java.util.List;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;

public record Badge(
    String code,
    List<BadgeLocale> locales
) {
    public void validateLocale() {
        if (!LocaleUtils.checkDefaultLanguage(locales)) {
            throw new IllegalStateException("Locale must have default language " + Language.DEFAULT + " at badge " + code);
        }
    }
}
