package ru.homyakin.seeker.game.item.modifier;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.item.modifier.models.ModifierLocale;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.WordForm;

class ModifierCompatibilityTest {
    @Test
    void attackObjectAcceptsAttackAndAnyButNotDefenseModifiers() {
        final var object = object(
            Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.SLASH, 1, 60)),
            Optional.empty()
        );

        Assertions.assertEquals(
            Set.of(ModifierType.ATTACK, ModifierType.ANY),
            ModifierCompatibility.compatibleTypes(object)
        );
        Assertions.assertTrue(ModifierCompatibility.isCompatible(
            object,
            modifier(ModifierType.ATTACK, PersonageSlot.MAIN_HAND),
            PersonageSlot.MAIN_HAND
        ));
        Assertions.assertFalse(ModifierCompatibility.isCompatible(
            object,
            modifier(ModifierType.DEFENSE, PersonageSlot.MAIN_HAND),
            PersonageSlot.MAIN_HAND
        ));
    }

    @Test
    void defenseObjectAcceptsDefenseAndAnyButNotAttackModifiers() {
        final var object = object(
            Set.of(PersonageSlot.BODY),
            Optional.empty(),
            Optional.of(new ItemDefense(DefenseType.CLOTH, 60))
        );

        Assertions.assertEquals(
            Set.of(ModifierType.DEFENSE, ModifierType.ANY),
            ModifierCompatibility.compatibleTypes(object)
        );
        Assertions.assertTrue(ModifierCompatibility.isCompatible(
            object,
            modifier(ModifierType.DEFENSE, PersonageSlot.BODY),
            PersonageSlot.BODY
        ));
        Assertions.assertFalse(ModifierCompatibility.isCompatible(
            object,
            modifier(ModifierType.ATTACK, PersonageSlot.BODY),
            PersonageSlot.BODY
        ));
    }

    @Test
    void modifierAndObjectMustBothContainSelectedSlot() {
        final var object = object(
            Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.SLASH, 1, 60)),
            Optional.empty()
        );
        final var modifier = modifier(ModifierType.ANY, PersonageSlot.MAIN_HAND);

        Assertions.assertFalse(ModifierCompatibility.isCompatible(object, modifier, PersonageSlot.OFF_HAND));
    }

    private static ItemObject object(
        Set<PersonageSlot> slots,
        Optional<ItemAttack> attack,
        Optional<ItemDefense> defense
    ) {
        return new ItemObject(
            "test-object",
            slots,
            attack,
            defense,
            attack.isPresent() ? 0 : 60,
            0,
            0,
            0,
            10,
            0,
            Map.of(Language.RU, new ItemObjectLocale("Предмет", WordForm.MASCULINE))
        );
    }

    private static Modifier modifier(ModifierType type, PersonageSlot slot) {
        return new Modifier(
            "test-modifier-" + type,
            ActiveEnum.BERSERK,
            type,
            Set.of(slot),
            Map.of(
                Language.RU,
                new ModifierLocale(Map.of(WordForm.WITHOUT, "Проверочный"))
            )
        );
    }
}
