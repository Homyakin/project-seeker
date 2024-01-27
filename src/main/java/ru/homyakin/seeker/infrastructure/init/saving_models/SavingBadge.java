package ru.homyakin.seeker.infrastructure.init.saving_models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import ru.homyakin.seeker.game.personage.badge.BadgeLocale;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Localized;

public record SavingBadge(
    @JsonProperty("code")
    BadgeView view,
    List<BadgeLocale> locales
) implements Localized<BadgeLocale> {
    public void validateLocale() {
        if (!LocaleUtils.checkDefaultLanguage(locales)) {
            throw new IllegalStateException("Locale must have default language " + Language.DEFAULT + " at view " + view.code());
        }
    }
}
