package ru.homyakin.seeker.game.random.item.action.shop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.action.pool.ItemRandomPoolRenew;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemParams;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemParamsWithoutRarity;
import ru.homyakin.seeker.game.random.item.entity.shop.ShopRandomPoolRepository;

@Component
public class PersonageNextShopItemParams {
    private final ShopRandomPoolRepository repository;
    private final ItemRandomPoolRenew randomPoolRenew;

    public PersonageNextShopItemParams(ShopRandomPoolRepository repository, ItemRandomPoolRenew randomPoolRenew) {
        this.repository = repository;
        this.randomPoolRenew = randomPoolRenew;
    }

    public FullItemParams getRandom(PersonageId personageId) {
        final var shopRandomPool = repository.getRandomPool(personageId);

        final var updatedShopRandomPool = randomPoolRenew.fullRenewIfEmpty(shopRandomPool);
        final var params = updatedShopRandomPool.next();
        repository.saveRandomPool(personageId, updatedShopRandomPool);

        return params;
    }

    public ItemParamsWithoutRarity getRarity(PersonageId personageId, ItemRarity rarity) {
        final var pool = repository.getRarityPool(personageId, rarity);

        final var updatedPool = randomPoolRenew.renewIfEmptyWithoutRarity(pool);
        final var params = updatedPool.next();
        repository.saveRarityPool(personageId, rarity, updatedPool);

        return params;
    }
}
