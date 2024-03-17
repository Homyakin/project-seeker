package ru.homyakin.seeker.game.item.models;

import java.util.Map;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;
import ru.homyakin.seeker.locale.WordForm;

public record Modifier(
    String code,
    ModifierType type,
    ItemRangeCharacteristics characteristics,
    Map<Language, ModifierLocale> locales
) implements Localized<ModifierLocale> {
    public void validateWordForms() {
        for (final var entry : locales.entrySet()) {
            final var forms = entry.getValue().form();
            WordForm.languageRequiredForms(entry.getKey())
                .ifPresentOrElse(
                    requiredForms -> {
                        if (!forms.keySet().containsAll(requiredForms)) {
                            if (!entry.getValue().form().containsKey(WordForm.WITHOUT)) {
                                throw new IllegalStateException(
                                    "Modifier %s doesn't contain required language %s forms".formatted(code, entry.getKey())
                                );
                            }
                        }
                    },
                    () -> {
                        if (!forms.containsKey(WordForm.WITHOUT)) {
                            throw new IllegalStateException(
                                "Modifier %s doesn't contain required language %s forms".formatted(code, entry.getKey())
                            );
                        }
                    }
                );
        }
    }
}
