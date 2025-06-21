package ru.homyakin.seeker.game.random.item.action;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.ItemParamsFull;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.ShopRandomPoolRepository;
import ru.homyakin.seeker.game.shop.models.ShopItemType;
import ru.homyakin.seeker.utils.RandomUtils;

import java.util.Optional;

@Component
public class PersonageNextShopItemParams {
    private final ShopRandomPoolRepository repository;
    private final ItemRandomPoolRenew randomPoolRenew;
    private final ItemRandomConfig config;

    public PersonageNextShopItemParams(
        ShopRandomPoolRepository repository,
        ItemRandomPoolRenew randomPoolRenew,
        ItemRandomConfig config
    ) {
        this.repository = repository;
        this.randomPoolRenew = randomPoolRenew;
        this.config = config;
    }

    public ItemParamsFull getForShopItemType(PersonageId personageId, ShopItemType itemType) {
        final var pool = repository.getPool(personageId, itemType);

        final var updatedPool = randomPoolRenew.fullRenewIfEmpty(pool);
        final var params = typeToRarity(itemType)
            .map(rarity -> updatedPool.next(
                config.shopModifierCountPicker().pick(RandomUtils::getWithMax),
                rarity
            ))
            .orElseGet(() -> updatedPool.next(
                config.shopModifierCountPicker().pick(RandomUtils::getWithMax),
                config.shopRarityPicker().pick(RandomUtils::getWithMax)
            ));
        repository.savePool(personageId, itemType, updatedPool);

        return params;
    }

    private Optional<ItemRarity> typeToRarity(ShopItemType type) {
        return Optional.ofNullable(switch (type) {
            case COMMON -> ItemRarity.COMMON;
            case UNCOMMON -> ItemRarity.UNCOMMON;
            case RARE -> ItemRarity.RARE;
            case EPIC -> ItemRarity.EPIC;
            case LEGENDARY -> ItemRarity.LEGENDARY;
            case RANDOM -> null;
        });
    }
}
