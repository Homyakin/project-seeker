package ru.homyakin.seeker.game.random.item.entity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public record ModifierCountRandomPool(
    Queue<Integer> pool
) {
    public static ModifierCountRandomPool generate(
        int zeroModifiersInPool,
        int oneModifiersInPool,
        int twoModifiersInPool
    ) {
        final var pool = new LinkedList<Integer>();

        for (int i = 0; i < zeroModifiersInPool; ++i) {
            pool.add(0);
        }
        for (int i = 0; i < oneModifiersInPool; ++i) {
            pool.add(1);
        }
        for (int i = 0; i < twoModifiersInPool; ++i) {
            pool.add(2);
        }
        Collections.shuffle(pool);

        return new ModifierCountRandomPool(pool);
    }

    public Integer next() {
        return pool.poll();
    }

    public boolean isEmpty() {
        return pool.isEmpty();
    }

    public static final ModifierCountRandomPool EMPTY = new ModifierCountRandomPool(new LinkedList<>());
}
