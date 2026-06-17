package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPool;
import ru.homyakin.seeker.game.shop.models.ShopItemType;

public interface ShopRandomPoolRepository {
    ItemRandomPool getPool(PersonageId personageId, ShopItemType itemType);

    void savePool(PersonageId personageId, ShopItemType itemType, ItemRandomPool itemRandomPool);
}
