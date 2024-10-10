package ru.homyakin.seeker.game.random.item.action.pool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPoolWithoutRarity;
import ru.homyakin.seeker.game.random.item.entity.pool.ModifierCountRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.ModifierPoolSettings;
import ru.homyakin.seeker.game.random.item.entity.pool.RarityRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ItemRandomPoolRenewTest {
    private final ItemRandomConfig config = Mockito.mock();
    private final ItemRandomPoolRenew randomPoolRenew = new ItemRandomPoolRenew(config);

    @Test
    public void When_NotEmptyRandomPool_Then_NotChanged() {
        final var pool = new FullItemRandomPool(
            new RarityRandomPool(new LinkedList<>(List.of(ItemRarity.COMMON, ItemRarity.UNCOMMON))),
            new SlotRandomPool(new LinkedList<>(List.of(PersonageSlot.HELMET, PersonageSlot.GLOVES))),
            new ModifierCountRandomPool(new LinkedList<>(List.of(1, 2)))
        );
        final var result = randomPoolRenew.fullRenewIfEmpty(pool);
        Assertions.assertEquals(pool, result);
    }

    @Test
    public void When_EmptyRandomPool_Then_GenerateNew() {
        Mockito.when(config.raritiesInPool()).thenReturn(Map.of(ItemRarity.COMMON, 2));
        Mockito.when(config.modifierPoolSettings())
            .thenReturn(new ModifierPoolSettings(2, 0, 0));
        Mockito.when(config.sameSlotsInPool()).thenReturn(1);

        final var result = randomPoolRenew.fullRenewIfEmpty(FullItemRandomPool.EMPTY);

        final var expectedRarity = new RarityRandomPool(new LinkedList<>(List.of(ItemRarity.COMMON, ItemRarity.COMMON)));
        final var expectedModifier = new ModifierCountRandomPool(new LinkedList<>(List.of(0, 0)));


        Assertions.assertEquals(expectedRarity, result.rarityRandomPool());
        Assertions.assertEquals(expectedModifier, result.modifierCountRandomPool());

        Assertions.assertEquals(PersonageSlot.values().length, result.slotRandomPool().pool().size());
        for (final var slot: PersonageSlot.values()) {
            Assertions.assertTrue(result.slotRandomPool().pool().contains(slot));
        }
    }

    @Test
    public void When_NotEmptyRandomPoolWithoutRarity_Then_NotChanged() {
        final var pool = new ItemRandomPoolWithoutRarity(
            new SlotRandomPool(new LinkedList<>(List.of(PersonageSlot.HELMET, PersonageSlot.GLOVES))),
            new ModifierCountRandomPool(new LinkedList<>(List.of(1, 2)))
        );
        final var result = randomPoolRenew.renewIfEmptyWithoutRarity(pool);
        Assertions.assertEquals(pool, result);
    }



    @Test
    public void When_EmptyRandomPoolWithoutRarity_Then_GenerateNew() {
        Mockito.when(config.modifierPoolSettings())
            .thenReturn(new ModifierPoolSettings(2, 0, 0));
        Mockito.when(config.sameSlotsInPool()).thenReturn(1);

        final var result = randomPoolRenew.renewIfEmptyWithoutRarity(ItemRandomPoolWithoutRarity.EMPTY);

        final var expectedModifier = new ModifierCountRandomPool(new LinkedList<>(List.of(0, 0)));

        Assertions.assertEquals(expectedModifier, result.modifierCountRandomPool());

        Assertions.assertEquals(PersonageSlot.values().length, result.slotRandomPool().pool().size());
        for (final var slot: PersonageSlot.values()) {
            Assertions.assertTrue(result.slotRandomPool().pool().contains(slot));
        }
    }
}
