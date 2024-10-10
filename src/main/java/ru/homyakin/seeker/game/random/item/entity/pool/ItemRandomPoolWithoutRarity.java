package ru.homyakin.seeker.game.random.item.entity.pool;

public record ItemRandomPoolWithoutRarity(
    SlotRandomPool slotRandomPool,
    ModifierCountRandomPool modifierCountRandomPool
) {
    public ItemParamsWithoutRarity next() {
        return new ItemParamsWithoutRarity(
            slotRandomPool.next(),
            modifierCountRandomPool.next()
        );
    }

    public static final ItemRandomPoolWithoutRarity EMPTY = new ItemRandomPoolWithoutRarity(
        SlotRandomPool.EMPTY,
        ModifierCountRandomPool.EMPTY
    );
}
