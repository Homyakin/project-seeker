package ru.homyakin.seeker.game.rumor;

import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LanguageObject;

public record RumorLocale(
    Language language,
    String text
) implements LanguageObject {
}
