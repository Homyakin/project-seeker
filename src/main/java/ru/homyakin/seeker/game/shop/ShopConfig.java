package ru.homyakin.seeker.game.shop;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.shop.models.ShopItem;
import ru.homyakin.seeker.game.shop.models.ShopItemType;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Validated
@ConfigurationProperties("homyakin.seeker.shop")
public class ShopConfig {
    @NotEmpty
    private final List<ShopItem.Buy> buyingItems = new ArrayList<>();
    @NotEmpty
    private final Map<ItemRarity, Money> sellingPrices = new HashMap<>();
    @NotNull
    private Integer sellDiscountDivider;
    private boolean isBuyingItemsSorted = false;

    public List<ShopItem.Buy> getBuyingItems() {
        if (isBuyingItemsSorted) {
            return buyingItems;
        }
        buyingItems.sort(Comparator.comparingInt(it -> it.type().priority));
        isBuyingItemsSorted = true;
        return buyingItems;
    }

    public Money sellingPriceByRarity(ItemRarity rarity) {
        return sellingPrices.getOrDefault(rarity, Money.zero());
    }

    public Money buyingPriceByType(ShopItemType type) {
        for (final var item: getBuyingItems()) {
            if (item.type() == type) {
                return item.price();
            }
        }
        throw new IllegalStateException("Can't find price for " + type);
    }

    public Money buyingPriceByRarity(ItemRarity rarity) {
        return buyingPriceByType(typeByRarity(rarity));
    }

    public void setCommonPrice(Integer commonPrice) {
        buyingItems.add(new ShopItem.Buy(Money.from(commonPrice), ShopItemType.COMMON));
        if (sellDiscountDivider != null) {
            sellingPrices.put(ItemRarity.COMMON, Money.from(commonPrice / sellDiscountDivider));
        }
    }

    public void setUncommonPrice(Integer uncommonPrice) {
        buyingItems.add(new ShopItem.Buy(Money.from(uncommonPrice), ShopItemType.UNCOMMON));
        if (sellDiscountDivider != null) {
            sellingPrices.put(ItemRarity.UNCOMMON, Money.from(uncommonPrice / sellDiscountDivider));
        }
    }

    public void setRarePrice(Integer rarePrice) {
        buyingItems.add(new ShopItem.Buy(Money.from(rarePrice), ShopItemType.RARE));
        if (sellDiscountDivider != null) {
            sellingPrices.put(ItemRarity.RARE, Money.from(rarePrice / sellDiscountDivider));
        }
    }

    public void setEpicPrice(Integer epicPrice) {
        buyingItems.add(new ShopItem.Buy(Money.from(epicPrice), ShopItemType.EPIC));
        if (sellDiscountDivider != null) {
            sellingPrices.put(ItemRarity.EPIC, Money.from(epicPrice / sellDiscountDivider));
        }
    }

    public void setLegendaryPrice(Integer legendaryPrice) {
        buyingItems.add(new ShopItem.Buy(Money.from(legendaryPrice), ShopItemType.LEGENDARY));
        if (sellDiscountDivider != null) {
            sellingPrices.put(ItemRarity.LEGENDARY, Money.from(legendaryPrice / sellDiscountDivider));
        }
    }

    public void setRandomPrice(Integer randomPrice) {
        buyingItems.add(new ShopItem.Buy(Money.from(randomPrice), ShopItemType.RANDOM));
    }

    public void setSellDiscountDivider(Integer sellDiscountDivider) {
        this.sellDiscountDivider = sellDiscountDivider;
        if (!buyingItems.isEmpty()) {
            fillSellingPrices();
        }
    }

    private void fillSellingPrices() {
        buyingItems.forEach(
            it -> {
                switch (it.type()) {
                    case COMMON -> sellingPrices.put(ItemRarity.COMMON, it.price().divide(sellDiscountDivider));
                    case UNCOMMON -> sellingPrices.put(ItemRarity.UNCOMMON, it.price().divide(sellDiscountDivider));
                    case RARE -> sellingPrices.put(ItemRarity.RARE, it.price().divide(sellDiscountDivider));
                    case EPIC -> sellingPrices.put(ItemRarity.EPIC, it.price().divide(sellDiscountDivider));
                    case LEGENDARY -> sellingPrices.put(ItemRarity.LEGENDARY, it.price().divide(sellDiscountDivider));
                }
            }
        );
    }

    private ShopItemType typeByRarity(ItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> ShopItemType.COMMON;
            case UNCOMMON -> ShopItemType.UNCOMMON;
            case RARE -> ShopItemType.RARE;
            case EPIC -> ShopItemType.EPIC;
            case LEGENDARY -> ShopItemType.LEGENDARY;
        };
    }
}
