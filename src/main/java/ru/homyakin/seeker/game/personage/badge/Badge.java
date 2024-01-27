package ru.homyakin.seeker.game.personage.badge;

import java.util.List;
import ru.homyakin.seeker.locale.Localized;

public record Badge(
    int id,
    BadgeView view,
    List<BadgeLocale> locales
) implements Localized<BadgeLocale> {
}
