package ru.homyakin.seeker.game.item.models;

import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record Modifier(
    int id,
    ModifierType type,
    Map<Language, ModifierLocale> locales
) implements Localized<ModifierLocale> {
}
