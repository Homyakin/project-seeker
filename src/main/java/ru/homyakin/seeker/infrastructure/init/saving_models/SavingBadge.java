package ru.homyakin.seeker.infrastructure.init.saving_models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import ru.homyakin.seeker.game.personage.badge.BadgeLocale;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record SavingBadge(
    @JsonProperty("code")
    BadgeView view,
    Map<Language, BadgeLocale> locales
) implements Localized<BadgeLocale> {
}
