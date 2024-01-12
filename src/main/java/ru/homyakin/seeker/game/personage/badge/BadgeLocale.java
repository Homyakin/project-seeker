package ru.homyakin.seeker.game.personage.badge;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LanguageObject;

public record BadgeLocale(
    Language language,
    String description
) implements LanguageObject {
}
