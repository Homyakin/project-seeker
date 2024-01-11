package ru.homyakin.seeker.game.personage.badge;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleObject;

public record BadgeLocale(
    Language language,
    String description
) implements LocaleObject {
}
