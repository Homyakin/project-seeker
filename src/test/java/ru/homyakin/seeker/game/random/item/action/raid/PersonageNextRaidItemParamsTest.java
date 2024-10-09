package ru.homyakin.seeker.game.random.item.action.raid;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.rarity.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemParams;
import ru.homyakin.seeker.game.random.item.entity.pool.FullItemRandomPool;
import ru.homyakin.seeker.game.random.item.action.pool.ItemRandomPoolRenew;
import ru.homyakin.seeker.game.random.item.entity.pool.ModifierCountRandomPool;
import ru.homyakin.seeker.game.random.item.entity.raid.RaidItemRandomPoolRepository;
import ru.homyakin.seeker.game.random.item.entity.pool.RarityRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;

import java.util.LinkedList;
import java.util.List;

public class PersonageNextRaidItemParamsTest {
    private final RaidItemRandomPoolRepository raidItemRandomPoolRepository = Mockito.mock();
    private final ItemRandomPoolRenew randomPoolRenew = Mockito.mock();
    private final PersonageNextRaidItemParams action = new PersonageNextRaidItemParams(
        raidItemRandomPoolRepository,
        randomPoolRenew
    );

    @Test
    public void When_RaidItemRandomPoolNotEmpty_Then_ReturnNextFromPoolAndSave() {
        // given
        final var personageId = PersonageId.from(1);
        final var pool = new FullItemRandomPool(
            new RarityRandomPool(new LinkedList<>(List.of(ItemRarity.COMMON, ItemRarity.UNCOMMON))),
            new SlotRandomPool(new LinkedList<>(List.of(PersonageSlot.HELMET, PersonageSlot.GLOVES))),
            new ModifierCountRandomPool(new LinkedList<>(List.of(1, 2)))
        );

        // when
        Mockito.when(raidItemRandomPoolRepository.get(personageId)).thenReturn(pool);
        Mockito.when(randomPoolRenew.renewIfEmpty(pool)).thenReturn(pool);
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
        Assertions.assertEquals(List.of(ItemRarity.UNCOMMON), captor.getValue().rarityRandomPool().pool());
        Assertions.assertEquals(List.of(PersonageSlot.GLOVES), captor.getValue().slotRandomPool().pool());
        Assertions.assertEquals(List.of(2), captor.getValue().modifierCountRandomPool().pool());
    }
}
