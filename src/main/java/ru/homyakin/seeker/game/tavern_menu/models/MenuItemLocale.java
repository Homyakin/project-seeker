package ru.homyakin.seeker.game.tavern_menu.models;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LanguageObject;

public record MenuItemLocale(
    Language language,
    String name,
    String[] consumeTemplate
) implements LanguageObject {
}
