package ru.homyakin.seeker.game.random.item.entity;

import ru.homyakin.seeker.game.personage.models.PersonageSlot;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Queue;

public record SlotRandomPool(
    Queue<PersonageSlot> pool
) {
    public static SlotRandomPool generate(int sameSlotsInPool) {
        final var availableSlots = Arrays.asList(PersonageSlot.values());

        final var pool = new LinkedList<PersonageSlot>();
        for (int i = 0; i < sameSlotsInPool; ++i) {
            Collections.shuffle(availableSlots);
            pool.addAll(availableSlots);
        }

        return new SlotRandomPool(pool);
    }

    public PersonageSlot next() {
        return pool.poll();
    }

    public boolean isEmpty() {
        return pool.isEmpty();
    }

    public static final SlotRandomPool EMPTY = new SlotRandomPool(new LinkedList<>());
}
