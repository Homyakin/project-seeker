package ru.homyakin.seeker.game.item.models;

import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;

public record GenerateModifier(
    int id,
    String code,
    ModifierType type,
    ItemRangeCharacteristics characteristics,
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
