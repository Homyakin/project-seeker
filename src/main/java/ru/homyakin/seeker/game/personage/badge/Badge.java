package ru.homyakin.seeker.game.personage.badge;

import java.util.List;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Localized;

public record Badge(
    String code,
    List<BadgeLocale> locales
) implements Localized<BadgeLocale> {
    public void validateLocale() {
        if (!LocaleUtils.checkDefaultLanguage(locales)) {
            throw new IllegalStateException("Locale must have default language " + Language.DEFAULT + " at badge " + code);
        }
    }
}
