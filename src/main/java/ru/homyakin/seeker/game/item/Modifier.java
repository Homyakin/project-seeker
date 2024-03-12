package ru.homyakin.seeker.game.item;

import java.util.Map;
import java.util.Optional;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record Modifier(
    String code,
    ModifierType type,
    Optional<Attack> attack,
    Map<Language, ModifierLocale> locales
) implements Localized<ModifierLocale> {
}
