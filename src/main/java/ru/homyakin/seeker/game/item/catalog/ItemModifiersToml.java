package ru.homyakin.seeker.game.item.catalog;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import ru.homyakin.seeker.game.battle.v4.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.item.modifier.models.ModifierLocale;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.Localized;
import ru.homyakin.seeker.locale.WordForm;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ItemModifiersToml(List<SavingModifier> modifier) {
    private static final TomlMapper MAPPER = TomlMapper.builder()
        .addModule(new Jdk8Module())
        .build();

    public static ItemModifiersToml load(InputStream stream) {
        try {
            return MAPPER.readValue(stream, ItemModifiersToml.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load item modifiers catalog toml", e);
        }
    }

    public List<Modifier> modifiers() {
        return modifier.stream().map(SavingModifier::toModifier).toList();
    }

    public record SavingModifier(
        String code,
        ActiveEnum activeEnum,
        ModifierType type,
        Set<PersonageSlot> slots,
        Map<Language, ModifierLocale> locales
    ) implements Localized<ModifierLocale> {
        Modifier toModifier() {
            return new Modifier(code, activeEnum, type, slots, locales);
        }

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
}
