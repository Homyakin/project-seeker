package ru.homyakin.seeker.game.random.item.action;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.entity.FullItemParams;
import ru.homyakin.seeker.game.random.item.entity.FullItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.ModifierCountRandomPool;
import ru.homyakin.seeker.game.random.item.entity.ModifierPoolSettings;
import ru.homyakin.seeker.game.random.item.entity.RaidItemRandomPoolRepository;
import ru.homyakin.seeker.game.random.item.entity.RarityRandomPool;
import ru.homyakin.seeker.game.random.item.entity.SlotRandomPool;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PersonageNextRaidItemParamsTest {
    private final RaidItemRandomPoolRepository raidItemRandomPoolRepository = Mockito.mock();
    private final ItemRandomConfig config = Mockito.mock();
    private final PersonageNextRaidItemParams action = new PersonageNextRaidItemParams(raidItemRandomPoolRepository, config);

    @Test
    public void When_RaidItemRandomPoolNotEmpty_Then_ReturnNextFromPoolAndSave() {
        // given
        final var personageId = PersonageId.from(1);
        final var pool = new FullItemRandomPool(
            new RarityRandomPool(new LinkedList<>(List.of(ItemRarity.COMMON))),
            new SlotRandomPool(new LinkedList<>(List.of(PersonageSlot.HELMET))),
            new ModifierCountRandomPool(new LinkedList<>(List.of(1)))
        );

        // when
        Mockito.when(raidItemRandomPoolRepository.get(personageId)).thenReturn(pool);
        final var result = action.get(personageId);
        final var captor = ArgumentCaptor.forClass(FullItemRandomPool.class);
        Mockito.verify(raidItemRandomPoolRepository, Mockito.times(1))
            .save(Mockito.eq(personageId), captor.capture());

        // then
        final var expected = new FullItemParams(
            ItemRarity.COMMON,
            PersonageSlot.HELMET,
            1
        );
        Assertions.assertEquals(expected, result);
        Assertions.assertTrue(captor.getValue().rarityRandomPool().isEmpty());
        Assertions.assertTrue(captor.getValue().slotRandomPool().isEmpty());
        Assertions.assertTrue(captor.getValue().modifierCountRandomPool().isEmpty());
    }

    @Test
    public void When_RaidItemRandomPoolIsEmpty_Then_ReturnGenerateNewAndSave() {
        // given
        final var personageId = PersonageId.from(1);
        final var pool = FullItemRandomPool.EMPTY;

        // when
        Mockito.when(raidItemRandomPoolRepository.get(personageId)).thenReturn(pool);
        Mockito.when(config.raritiesInPool()).thenReturn(Map.of(ItemRarity.COMMON, 2));
        Mockito.when(config.modifierPoolSettings())
            .thenReturn(new ModifierPoolSettings(2, 0, 0));
        Mockito.when(config.sameSlotsInPool()).thenReturn(1);
        final var result = action.get(personageId);
        final var captor = ArgumentCaptor.forClass(FullItemRandomPool.class);
        Mockito.verify(raidItemRandomPoolRepository, Mockito.times(1))
            .save(Mockito.eq(personageId), captor.capture());

        // then
        Assertions.assertEquals(ItemRarity.COMMON, result.rarity());
        Assertions.assertEquals(0, result.modifiersCount());

        Assertions.assertEquals(1, captor.getValue().rarityRandomPool().pool().size());
        Assertions.assertEquals(ItemRarity.COMMON, captor.getValue().rarityRandomPool().next());
        Assertions.assertEquals(1, captor.getValue().modifierCountRandomPool().pool().size());
        Assertions.assertEquals(0, captor.getValue().modifierCountRandomPool().next());

        Assertions.assertEquals(PersonageSlot.values().length - 1, captor.getValue().slotRandomPool().pool().size());
    }
}
