package ru.homyakin.seeker.game.random.item.action.raid;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.action.PersonageNextRaidItemParams;
import ru.homyakin.seeker.game.random.item.entity.ItemParamsFull;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPool;
import ru.homyakin.seeker.game.random.item.action.ItemRandomPoolRenew;
import ru.homyakin.seeker.game.random.item.entity.RaidItemRandomPoolRepository;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;
import ru.homyakin.seeker.utils.ProbabilityPicker;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PersonageNextRaidItemParamsTest {
    private final RaidItemRandomPoolRepository raidItemRandomPoolRepository = Mockito.mock();
    private final ItemRandomPoolRenew randomPoolRenew = Mockito.mock();
    private final ItemRandomConfig config = Mockito.mock();
    private final PersonageNextRaidItemParams action = new PersonageNextRaidItemParams(
        raidItemRandomPoolRepository,
        randomPoolRenew,
        config
    );

    @Test
    public void When_RaidItemRandomPoolNotEmpty_Then_ReturnNextFromPoolAndSave() {
        // given
        final var personageId = PersonageId.from(1);
        final var pool = new ItemRandomPool(
            new SlotRandomPool(new LinkedList<>(List.of(PersonageSlot.HELMET, PersonageSlot.GLOVES)))
        );

        // when
        Mockito.when(raidItemRandomPoolRepository.get(personageId)).thenReturn(pool);
        Mockito.when(randomPoolRenew.fullRenewIfEmpty(pool)).thenReturn(pool);
        Mockito.when(config.raidRarityPicker()).thenReturn(rarityPicker());
        Mockito.when(config.raidModifierCountPicker()).thenReturn(modifierPicker());
        final var result = action.get(personageId);
        final var captor = ArgumentCaptor.forClass(ItemRandomPool.class);
        Mockito.verify(raidItemRandomPoolRepository, Mockito.times(1))
            .save(Mockito.eq(personageId), captor.capture());

        // then
        final var expected = new ItemParamsFull(
            ItemRarity.COMMON,
            PersonageSlot.HELMET,
            1
        );
        Assertions.assertEquals(expected, result);
        Assertions.assertEquals(List.of(PersonageSlot.GLOVES), captor.getValue().slotRandomPool().pool());
    }

    private ProbabilityPicker<ItemRarity> rarityPicker() {
        return new ProbabilityPicker<>(
            Map.of(ItemRarity.COMMON, 1)
        );
    }

    private ProbabilityPicker<Integer> modifierPicker() {
        return new ProbabilityPicker<>(
            Map.of(1, 1)
        );
    }
}
