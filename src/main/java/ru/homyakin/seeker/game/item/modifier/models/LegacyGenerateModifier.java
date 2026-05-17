package ru.homyakin.seeker.game.item.modifier.models;

import java.util.Map;

import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record LegacyGenerateModifier(
    int id,
    String code,
    LegacyModifierType type,
    ModifierGenerateCharacteristics characteristics,
    Map<Language, ModifierLocale> locales
) implements Localized<ModifierLocale> {
    public LegacyModifier toModifier() {
        return new LegacyModifier(
            id,
            type,
            locales
        );
    }
}
