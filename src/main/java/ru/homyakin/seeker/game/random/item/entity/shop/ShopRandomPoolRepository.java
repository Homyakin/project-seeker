package ru.homyakin.seeker.game.random.item.entity.shop;

import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPoolWithoutRarity;

public interface ShopRandomPoolRepository {
    FullItemRandomPool getRandomPool(PersonageId personageId);

    void saveRandomPool(PersonageId personageId, FullItemRandomPool itemRandomPool);

    ItemRandomPoolWithoutRarity getRarityPool(PersonageId personageId, ItemRarity rarity);

    void saveRarityPool(PersonageId personageId, ItemRarity rarity, ItemRandomPoolWithoutRarity itemRandomPool);
}
