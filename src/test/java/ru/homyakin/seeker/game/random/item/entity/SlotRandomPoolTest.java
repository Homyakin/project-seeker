package ru.homyakin.seeker.game.random.item.entity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

import java.util.HashMap;
import java.util.HashSet;

public class SlotRandomPoolTest {
    @Test
    public void When_GeneratePoolWithParams_Then_PoolContainsRequiredCount() {
        final var pool = SlotRandomPool.generate(2);

        final var resultMap = new HashMap<PersonageSlot, Integer>();
        for (final var slot : PersonageSlot.values()) {
            resultMap.put(slot, 0);
        }
        pool.pool().forEach(slot -> resultMap.put(slot, resultMap.get(slot) + 1));

        Assertions.assertEquals(PersonageSlot.values().length, new HashSet<>(pool.pool()).size());

        for (final var entry : resultMap.entrySet()) {
            Assertions.assertEquals(2, entry.getValue());
        }
    }
}
