package ru.homyakin.seeker.game.item.modifier.models;

import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record LegacyModifier(
    int id,
    LegacyModifierType type,
    Map<Language, ModifierLocale> locales
) implements Localized<ModifierLocale> {
}
