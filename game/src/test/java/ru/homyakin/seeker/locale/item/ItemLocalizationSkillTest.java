package ru.homyakin.seeker.locale.item;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.item.loadout.entity.EquipmentLoadout;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Inventory;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.item.models.PutOnItemResult;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.item.modifier.models.ModifierLocale;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.locale.WordForm;

class ItemLocalizationSkillTest {
    @BeforeAll
    static void initLocalization() {
        LocalizationInitializer.initLocale();
    }

    @Test
    void fullItemShowsContributionAndEffectForExplicitFormulaVersion() {
        final var item = itemWithThreeSkillPoints();

        final var legacy = ItemLocalization.fullItem(Language.RU, item);
        final var scaling = ItemLocalization.fullItem(
            Language.RU,
            item,
            SkillFormulaVersion.SCALING_SKILLS_V1
        );

        Assertions.assertAll(
            () -> Assertions.assertTrue(legacy.contains("Двойная атака (II, 3 оч.)")),
            () -> Assertions.assertTrue(legacy.contains("15 дополнительного урона")),
            () -> Assertions.assertTrue(scaling.contains("Двойная атака (II, 3 оч.)")),
            () -> Assertions.assertTrue(scaling.contains("30% сохранённой атаки")),
            () -> Assertions.assertFalse(scaling.contains("15 дополнительного урона")),
            () -> Assertions.assertFalse(scaling.contains("${"))
        );
    }

    @Test
    void inventoryLoadoutAndEquipmentActionsPropagateTheExplicitSecondFormula() {
        final var equipped = personageItem(true);
        final var bagItem = personageItem(false);
        final var inventory = new Inventory(List.of(equipped));
        final var loadout = new EquipmentLoadout(
            1L, PersonageId.from(1L), "Проверка", List.of(equipped.id()), Position.FRONT,
            TargetingTactic.THREAT, Set.of()
        );
        final var version = SkillFormulaVersion.SCALING_SKILLS_V2;
        final var texts = List.of(
            ItemLocalization.equipment(Language.RU, inventory, version),
            ItemLocalization.bag(Language.RU, new Inventory(List.of(bagItem)), 0, version),
            ItemLocalization.loadoutDetail(Language.RU, loadout, inventory, false, version),
            ItemLocalization.initCreateLoadout(Language.RU, inventory, false, version),
            ItemLocalization.successPutOn(Language.RU, new PutOnItemResult(equipped, List.of(bagItem)), version),
            ItemLocalization.successTakeOff(Language.RU, equipped, version),
            ItemLocalization.fullItem(Language.RU, equipped, "/test", version),
            ItemLocalization.fullItemForShopSlot(Language.RU, equipped, PersonageSlot.MAIN_HAND, "/test", version)
        );

        for (final var text : texts) {
            Assertions.assertTrue(text.contains("15% сохранённой атаки"), text);
            Assertions.assertFalse(text.contains("15 дополнительного урона"), text);
            Assertions.assertFalse(text.contains("${"), text);
        }
        Assertions.assertTrue(ItemLocalization.equipment(Language.RU, inventory).contains("15 дополнительного урона"));
        Assertions.assertTrue(ItemLocalization.bag(Language.RU, new Inventory(List.of(bagItem)), 0)
            .contains("15 дополнительного урона"));
    }

    private static PersonageItem personageItem(boolean equipped) {
        final var item = itemWithThreeSkillPoints();
        return new PersonageItem(
            1L, 1, item.object(), Optional.of(1), item.modifier(), item.rarity(),
            Optional.of(PersonageId.from(1L)), equipped, 0
        );
    }

    private static Item itemWithThreeSkillPoints() {
        final var object = new ItemObject(
            "test_blade",
            Set.of(PersonageSlot.MAIN_HAND),
            List.of(new ItemAttack(AttackType.SLASH, 1, 1, 120)),
            Optional.empty(),
            0,
            0,
            0,
            0,
            0,
            0,
            0,
            ItemProgressionVersion.V1,
            Map.of(Language.RU, new ItemObjectLocale("Проверочный клинок", WordForm.MASCULINE))
        );
        final var modifier = new Modifier(
            "double",
            ActiveEnum.DOUBLE_ATTACK,
            ModifierType.ATTACK,
            Set.of(PersonageSlot.MAIN_HAND),
            Map.of(Language.RU, new ModifierLocale(Map.of(WordForm.MASCULINE, "Двойной")))
        );
        return new Item(object, Optional.of(modifier), ItemRarity.EPIC);
    }
}
