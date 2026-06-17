package ru.homyakin.seeker.game.shop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.shop.models.ShopItemType;

class ShopConfigTest {
    private final ShopConfig config = new ShopConfig();

    @BeforeEach
    void setUp() {
        config.setCommonPrice(50);
        config.setUncommonPrice(100);
        config.setSellDiscountDivider(2);
    }

    @Test
    void specificObjectUnitPrice_isTwiceCommonBoxPrice() {
        Assertions.assertEquals(100, config.specificObjectUnitPrice().value());
    }

    @Test
    void specificObjectBuyPrice_multipliesUnitPriceBySlotCount() {
        Assertions.assertEquals(100, config.specificObjectBuyPrice(1).value());
        Assertions.assertEquals(200, config.specificObjectBuyPrice(2).value());
    }

    @Test
    void specificObjectBuyPrice_usesAtLeastOneSlot() {
        Assertions.assertEquals(100, config.specificObjectBuyPrice(0).value());
    }

    @Test
    void buyingPriceByType_returnsConfiguredPrice() {
        Assertions.assertEquals(50, config.buyingPriceByType(ShopItemType.COMMON).value());
        Assertions.assertEquals(100, config.buyingPriceByType(ShopItemType.UNCOMMON).value());
    }
}
