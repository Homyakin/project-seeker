package ru.homyakin.seeker.game.item.modifier.models;

import java.util.Map;

import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record GenerateModifier(
    int id,
    String code,
    ModifierType type,
    ModifierGenerateCharacteristics characteristics,
    Map<Language, ModifierLocale> locales
) implements Localized<ModifierLocale> {
    public Modifier toModifier() {
        return new Modifier(
            id,
            type,
            locales
        );
    }
}
