package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.item.rarity.ItemRarity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

public record RarityRandomPool(
    Queue<ItemRarity> pool
) {
    public static RarityRandomPool generate(
        Map<ItemRarity, Integer> itemRarityInPoolCount
    ) {
        final var pool = new LinkedList<ItemRarity>();

        for (final var entry : itemRarityInPoolCount.entrySet()) {
            for (int i = 0; i < entry.getValue(); ++i) {
                pool.add(entry.getKey());
            }
        }
        Collections.shuffle(pool);
        return new RarityRandomPool(pool);
    }

    public ItemRarity next() {
        return pool.poll();
    }

    public boolean isEmpty() {
        return pool.isEmpty();
    }

    public static final RarityRandomPool EMPTY = new RarityRandomPool(new LinkedList<>());
}
