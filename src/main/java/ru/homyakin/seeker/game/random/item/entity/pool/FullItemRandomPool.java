package ru.homyakin.seeker.game.random.item.entity.pool;

public record FullItemRandomPool(
    RarityRandomPool rarityRandomPool,
    SlotRandomPool slotRandomPool,
    ModifierCountRandomPool modifierCountRandomPool
) {

    public FullItemParams next() {
        return new FullItemParams(
            rarityRandomPool.next(),
            slotRandomPool.next(),
            modifierCountRandomPool.next()
        );
    }

    public static final FullItemRandomPool EMPTY = new FullItemRandomPool(
        RarityRandomPool.EMPTY,
        SlotRandomPool.EMPTY,
        ModifierCountRandomPool.EMPTY
    );
}
