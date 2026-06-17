package ru.homyakin.seeker.game.random.item.action.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.action.ItemRandomPoolRenew;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;

import java.util.LinkedList;
import java.util.List;
public class ItemRandomPoolRenewTest {
    private final ItemRandomConfig config = Mockito.mock();
    private final ItemRandomPoolRenew randomPoolRenew = new ItemRandomPoolRenew(config);

    @Test
    public void When_NotEmptyRandomPool_Then_NotChanged() {
        final var pool = new ItemRandomPool(
            new SlotRandomPool(new LinkedList<>(List.of(PersonageSlot.HELMET, PersonageSlot.GLOVES)))
        );
        final var result = randomPoolRenew.fullRenewIfEmpty(pool);
        Assertions.assertEquals(pool, result);
    }

    @Test
    public void When_EmptyRandomPool_Then_GenerateNew() {
        Mockito.when(config.sameSlotsInPool()).thenReturn(1);

        final var result = randomPoolRenew.fullRenewIfEmpty(ItemRandomPool.EMPTY);

        Assertions.assertEquals(PersonageSlot.values().length, result.slotRandomPool().pool().size());
        for (final var slot: PersonageSlot.values()) {
            Assertions.assertTrue(result.slotRandomPool().pool().contains(slot));
        }
    }
}
