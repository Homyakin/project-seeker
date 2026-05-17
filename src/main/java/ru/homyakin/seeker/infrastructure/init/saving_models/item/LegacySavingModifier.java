package ru.homyakin.seeker.infrastructure.init.saving_models.item;

import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;
import ru.homyakin.seeker.game.item.characteristics.models.ModifierGenerateCharacteristics;
import ru.homyakin.seeker.game.item.modifier.models.ModifierLocale;
import ru.homyakin.seeker.game.item.modifier.models.LegacyModifierType;
import ru.homyakin.seeker.game.item.models.LegacyItemRarity;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;
import ru.homyakin.seeker.locale.WordForm;

public record LegacySavingModifier(
    @JsonProperty(required = true)
    String code,
    @JsonProperty(required = true)
    LegacyModifierType type,
    @JsonProperty(required = true)
    Set<LegacyItemRarity> rarities,
    @JsonProperty(required = true)
    ModifierGenerateCharacteristics characteristics,
    @JsonProperty(required = true)
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
                                    "LegacyModifier %s doesn't contain required language %s forms".formatted(code, entry.getKey())
                                );
                            }
                        }
                    },
                    () -> {
                        if (!forms.containsKey(WordForm.WITHOUT)) {
                            throw new IllegalStateException(
                                "LegacyModifier %s doesn't contain required language %s forms".formatted(code, entry.getKey())
                            );
                        }
                    }
                );
        }
    }
}
