package ru.homyakin.seeker.game.personage.badge;

import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record Badge(
    int id,
    BadgeView view,
    Map<Language, BadgeLocale> locales
) implements Localized<BadgeLocale> {
}
