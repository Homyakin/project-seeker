package ru.homyakin.seeker.infrastructure.init.saving_models;

import java.util.List;
import ru.homyakin.seeker.game.personage.badge.BadgeLocale;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Localized;

public record SavingBadge(
    String code,
    List<BadgeLocale> locales
) implements Localized<BadgeLocale> {
    public void validateLocale() {
        if (!LocaleUtils.checkDefaultLanguage(locales)) {
            throw new IllegalStateException("Locale must have default language " + Language.DEFAULT + " at badge " + code);
        }
    }
}
