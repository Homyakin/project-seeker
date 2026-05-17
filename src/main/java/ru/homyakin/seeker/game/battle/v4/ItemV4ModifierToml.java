package ru.homyakin.seeker.game.battle.v4;

import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import ru.homyakin.seeker.game.battle.v4.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.modifier.models.ModifierLocale;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record ItemV4ModifierToml(List<SavingV4Modifier> modifier) {
    private static final TomlMapper MAPPER = TomlMapper.builder()
        .addModule(new Jdk8Module())
        .build();

    public static ItemV4ModifierToml load(InputStream stream) {
        try {
            return MAPPER.readValue(stream, ItemV4ModifierToml.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load item v4 modifier toml", e);
        }
    }

    public List<Modifier> modifiers() {
        return modifier.stream().map(SavingV4Modifier::toModifier).toList();
    }

    public record SavingV4Modifier(
        String code,
        ActiveEnum activeEnum,
        Modifier.ModifierType type,
        Set<PersonageSlot> slots,
        Map<Language, ModifierLocale> locales
    ) {
        Modifier toModifier() {
            return new Modifier(code, activeEnum, type, slots, locales);
        }
    }
}
