package ru.homyakin.seeker.locale.shop;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.DefenseType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.game.item.models.ItemDefense;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemObjectLocale;
import ru.homyakin.seeker.game.item.models.ItemProgressionVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.item.storm.ItemEnhancementDelta;
import ru.homyakin.seeker.game.item.storm.StormEnhanceProbabilities;
import ru.homyakin.seeker.game.models.StormShards;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.shop.models.AvailableAction;
import ru.homyakin.seeker.game.shop.models.StormEnhanceAction;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.locale.WordForm;

class ShopLocalizationStormEnhanceTest {
    @BeforeAll
    static void initLocalization() {
        LocalizationInitializer.initLocale();
    }

    @Test
    void stormEnhancementShowsPriceAndEveryExactNextLevelChange() {
        final var object = new ItemObject(
            "test_item",
            Set.of(PersonageSlot.BODY),
            List.of(
                new ItemAttack(AttackType.SLASH, 1, 1, 60),
                new ItemAttack(AttackType.MAGICAL, 2, 3, 120)
            ),
            Optional.of(new ItemDefense(DefenseType.ARCANE, 180)),
            240,
            0,
            0,
            0,
            0,
            0,
            0,
            ItemProgressionVersion.V1,
            Map.of(Language.RU, new ItemObjectLocale("Проверочный предмет", WordForm.MASCULINE))
        );
        final var item = new PersonageItem(
            7,
            3,
            object,
            Optional.empty(),
            Optional.empty(),
            ItemRarity.LEGENDARY,
            Optional.empty(),
            false,
            10,
            4
        );
        final var stormAction = new StormEnhanceAction(
            StormShards.from(123),
            new StormEnhanceProbabilities(40, 35, 25),
            ItemEnhancementDelta.next(object, 10),
            10,
            11,
            4
        );

        final var text = ShopLocalization.enhanceItemInfo(
            Language.RU,
            new AvailableAction(Optional.empty(), Optional.of(stormAction), item)
        );

        Assertions.assertAll(
            () -> Assertions.assertTrue(text.contains("123")),
            () -> Assertions.assertTrue(text.contains("Точная прибавка")),
            () -> Assertions.assertTrue(text.contains("+4")),
            () -> Assertions.assertTrue(text.contains("+1 (дист. 1)")),
            () -> Assertions.assertTrue(text.contains("+2 (дист. 2–3)")),
            () -> Assertions.assertTrue(text.contains("+3")),
            () -> Assertions.assertTrue(text.contains("Успех: 40%")),
            () -> Assertions.assertTrue(text.contains("Неудача 35%")),
            () -> Assertions.assertTrue(text.contains("Откат 25%"))
        );
    }
}
