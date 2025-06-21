package ru.homyakin.seeker.game.random.item.entity.pool;

import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.random.item.entity.ItemParamsFull;

public record ItemRandomPool(
    SlotRandomPool slotRandomPool
) {
    public ItemParamsFull next(int modifiersCount, ItemRarity rarity) {
        return new ItemParamsFull(
            rarity,
            slotRandomPool.next(),
            modifiersCount
        );
    }

    public static final ItemRandomPool EMPTY = new ItemRandomPool(
        SlotRandomPool.EMPTY
    );
}
