package ru.homyakin.seeker.locale.shop;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.shop.models.AvailableAction;
import ru.homyakin.seeker.game.shop.models.ShopItem;
import ru.homyakin.seeker.game.shop.models.SoldItem;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocalizationInitializer;
import ru.homyakin.seeker.locale.outpost.OutpostLocalization;

class ShopFormulaVersionLocalizationTest {
    @BeforeAll
    static void initLocalization() {
        LocalizationInitializer.initLocale();
    }

    @Test
    void everyFullShopAndDonationCardUsesExactSecondVersionNumbers() {
        final var item = epicBlade();
        final var version = SkillFormulaVersion.SCALING_SKILLS_V2;
        final var action = new AvailableAction(Optional.empty(), item);
        final var sold = new SoldItem(item, Money.from(100), List.of("Запасной"));
        final var texts = List.of(
            ShopLocalization.successBuy(Language.RU, item, version),
            ShopLocalization.successSell(Language.RU, sold, version),
            ShopLocalization.confirmSellLoadout(Language.RU, item, sold.price(), List.of("Запасной"), version),
            ShopLocalization.enhanceItemInfo(Language.RU, action, version),
            ShopLocalization.successAddModifier(Language.RU, action, version),
            ShopLocalization.successUpgradeRarity(Language.RU, action, version),
            ShopLocalization.successStormEnhance(Language.RU, action, version),
            ShopLocalization.failedStormEnhance(Language.RU, action, version),
            ShopLocalization.rollbackStormEnhance(Language.RU, action, version),
            ShopLocalization.menu(
                Language.RU, List.of(new ShopItem.Sell(sold.price(), item)), Optional.empty(), false, 0, version
            ),
            OutpostLocalization.confirmDonateLoadout(Language.RU, item, 25, List.of("Запасной"), version)
        );

        for (final var text : texts) {
            Assertions.assertTrue(text.contains("Двойная атака (II, 3 оч.)"), text);
            Assertions.assertTrue(text.contains("15% сохранённой атаки"), text);
            Assertions.assertFalse(text.contains("15 дополнительного урона"), text);
            Assertions.assertFalse(text.contains("30% сохранённой атаки"), text);
            Assertions.assertFalse(text.contains("${"), text);
            Assertions.assertFalse(text.contains("null"), text);
        }
    }

    @Test
    void legacyOverloadsKeepExistingCardsUntilExplicitSwitch() {
        final var item = epicBlade();
        final var action = new AvailableAction(Optional.empty(), item);
        final var sold = new SoldItem(item, Money.from(100), List.of());
        final var texts = List.of(
            ShopLocalization.successBuy(Language.RU, item),
            ShopLocalization.successSell(Language.RU, sold),
            ShopLocalization.confirmSellLoadout(Language.RU, item, sold.price(), List.of("Запасной")),
            ShopLocalization.enhanceItemInfo(Language.RU, action),
            ShopLocalization.successAddModifier(Language.RU, action),
            ShopLocalization.successUpgradeRarity(Language.RU, action),
            ShopLocalization.successStormEnhance(Language.RU, action),
            ShopLocalization.failedStormEnhance(Language.RU, action),
            ShopLocalization.rollbackStormEnhance(Language.RU, action),
            ShopLocalization.menu(Language.RU, List.of(new ShopItem.Sell(sold.price(), item)), Optional.empty(), false, 0),
            OutpostLocalization.confirmDonateLoadout(Language.RU, item, 25, List.of("Запасной"))
        );
        for (final var text : texts) {
            Assertions.assertTrue(text.contains("15 дополнительного урона"), text);
            Assertions.assertFalse(text.contains("15% сохранённой атаки"), text);
        }
    }

    private static PersonageItem epicBlade() {
        final var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V4);
        final var object = catalog.itemObjects().stream().filter(item -> item.code().equals("rapier")).findFirst().orElseThrow();
        final var modifier = catalog.modifiers().stream()
            .filter(item -> item.activeEnum() == ActiveEnum.DOUBLE_ATTACK).findFirst().orElseThrow();
        return new PersonageItem(
            1L, 1, object, Optional.of(1), Optional.of(modifier), ItemRarity.EPIC, Optional.empty(), false, 0
        );
    }
}
