package ru.homyakin.seeker.game.item.catalog;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.active_impl.SkillMapper;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.WordForm;

class ScalingItemModifiersCatalogTest {
    private static final Set<String> SCALING_CODES = Set.of(
        "guarding",
        "penetrating",
        "charging",
        "disrupting"
    );
    private static final Set<ActiveEnum> SCALING_SKILLS = Set.of(
        ActiveEnum.GUARD,
        ActiveEnum.PENETRATION,
        ActiveEnum.ACCUMULATION,
        ActiveEnum.TEMPO_BREAK
    );

    @Test
    void scalingCatalogContainsOnlyFourApprovedModifiers() {
        final var catalog = load("game-data/item_modifiers_scaling_v1.toml");
        catalog.modifier().forEach(ItemModifiersToml.SavingModifier::validateWordForms);
        final Map<String, Modifier> byCode = catalog.modifiers().stream()
            .collect(Collectors.toMap(Modifier::code, Function.identity()));

        Assertions.assertEquals(SCALING_CODES, byCode.keySet());
        assertModifier(
            byCode.get("guarding"),
            ActiveEnum.GUARD,
            ModifierType.DEFENSE,
            Set.of(PersonageSlot.OFF_HAND, PersonageSlot.BODY),
            List.of(
                "Прикрывающий",
                "Прикрывающая",
                "Прикрывающее",
                "Прикрывающие"
            )
        );
        assertModifier(
            byCode.get("penetrating"),
            ActiveEnum.PENETRATION,
            ModifierType.ATTACK,
            Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
            List.of(
                "Проникающий",
                "Проникающая",
                "Проникающее",
                "Проникающие"
            )
        );
        assertModifier(
            byCode.get("charging"),
            ActiveEnum.ACCUMULATION,
            ModifierType.ATTACK,
            Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
            List.of(
                "Накопительный",
                "Накопительная",
                "Накопительное",
                "Накопительные"
            )
        );
        assertModifier(
            byCode.get("disrupting"),
            ActiveEnum.TEMPO_BREAK,
            ModifierType.ANY,
            Set.of(PersonageSlot.MAIN_HAND, PersonageSlot.OFF_HAND),
            List.of(
                "Срывающий",
                "Срывающая",
                "Срывающее",
                "Срывающие"
            )
        );
    }

    @Test
    void productionCatalogRemainsLegacyOnly() {
        final var modifiers = load("game-data/item_modifiers_catalog.toml").modifiers();

        Assertions.assertEquals(11, modifiers.size());
        Assertions.assertTrue(modifiers.stream().map(Modifier::code).noneMatch(SCALING_CODES::contains));
        Assertions.assertTrue(modifiers.stream().map(Modifier::activeEnum).noneMatch(SCALING_SKILLS::contains));
    }

    @Test
    void legacySkillMapperRejectsEveryScalingOnlySkill() {
        for (final var skill : SCALING_SKILLS) {
            final var exception = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> SkillMapper.map(skill, 1)
            );
            Assertions.assertTrue(exception.getMessage().contains(skill.name()));
        }
    }

    private static void assertModifier(
        Modifier modifier,
        ActiveEnum activeEnum,
        ModifierType type,
        Set<PersonageSlot> slots,
        List<String> forms
    ) {
        Assertions.assertNotNull(modifier);
        Assertions.assertEquals(activeEnum, modifier.activeEnum());
        Assertions.assertEquals(type, modifier.type());
        Assertions.assertEquals(slots, modifier.availableOnSlots());
        final var russianForms = modifier.locales().get(Language.RU).form();
        Assertions.assertEquals(forms.get(0), russianForms.get(WordForm.MASCULINE));
        Assertions.assertEquals(forms.get(1), russianForms.get(WordForm.FEMININE));
        Assertions.assertEquals(forms.get(2), russianForms.get(WordForm.NEUTER));
        Assertions.assertEquals(forms.get(3), russianForms.get(WordForm.PLURAL));
    }

    private static ItemModifiersToml load(String path) {
        final var classLoader = ScalingItemModifiersCatalogTest.class.getClassLoader();
        try (final InputStream stream = classLoader.getResourceAsStream(path)) {
            Assertions.assertNotNull(stream, "Missing test resource: " + path);
            return ItemModifiersToml.load(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
