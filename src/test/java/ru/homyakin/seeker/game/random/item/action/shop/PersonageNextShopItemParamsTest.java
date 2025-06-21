package ru.homyakin.seeker.game.random.item.action.shop;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.random.item.action.ItemRandomPoolRenew;
import ru.homyakin.seeker.game.random.item.action.PersonageNextShopItemParams;
import ru.homyakin.seeker.game.random.item.entity.ItemParamsFull;
import ru.homyakin.seeker.game.random.item.entity.ItemRandomConfig;
import ru.homyakin.seeker.game.random.item.entity.pool.ItemRandomPool;
import ru.homyakin.seeker.game.random.item.entity.pool.SlotRandomPool;
import ru.homyakin.seeker.game.random.item.entity.ShopRandomPoolRepository;
import ru.homyakin.seeker.game.shop.models.ShopItemType;
import ru.homyakin.seeker.utils.ProbabilityPicker;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class PersonageNextShopItemParamsTest {
    private final ShopRandomPoolRepository shopRandomPoolRepository = Mockito.mock();
    private final ItemRandomPoolRenew randomPoolRenew = Mockito.mock();
    private final ItemRandomConfig config = Mockito.mock();
    private final PersonageNextShopItemParams action = new PersonageNextShopItemParams(
        shopRandomPoolRepository,
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
        final var itemType = ShopItemType.RANDOM;

        // when
        Mockito.when(shopRandomPoolRepository.getPool(personageId, itemType)).thenReturn(pool);
        Mockito.when(randomPoolRenew.fullRenewIfEmpty(pool)).thenReturn(pool);
        Mockito.when(config.shopRarityPicker()).thenReturn(rarityPicker());
        Mockito.when(config.shopModifierCountPicker()).thenReturn(modifierPicker());
        final var result = action.getForShopItemType(personageId, ShopItemType.RANDOM);
        final var captor = ArgumentCaptor.forClass(ItemRandomPool.class);
        Mockito.verify(shopRandomPoolRepository, Mockito.times(1))
            .savePool(Mockito.eq(personageId), Mockito.eq(itemType), captor.capture());

        // then
        final var expected = new ItemParamsFull(
            ItemRarity.COMMON,
            PersonageSlot.HELMET,
            1
        );
        Assertions.assertEquals(expected, result);
        Assertions.assertEquals(List.of(PersonageSlot.GLOVES), captor.getValue().slotRandomPool().pool());
    }

    @Test
    public void When_RaidItemRandomPoolWithoutRarityNotEmpty_Then_ReturnNextFromPoolAndSave() {
        // given
        final var personageId = PersonageId.from(1);
        final var pool = new ItemRandomPool(
            new SlotRandomPool(new LinkedList<>(List.of(PersonageSlot.HELMET, PersonageSlot.GLOVES)))
        );
        final var itemType = ShopItemType.LEGENDARY;

        // when
        Mockito.when(shopRandomPoolRepository.getPool(personageId, itemType)).thenReturn(pool);
        Mockito.when(randomPoolRenew.fullRenewIfEmpty(pool)).thenReturn(pool);
        Mockito.when(config.shopRarityPicker()).thenReturn(rarityPicker());
        Mockito.when(config.shopModifierCountPicker()).thenReturn(modifierPicker());
        final var result = action.getForShopItemType(personageId, itemType);
        final var captor = ArgumentCaptor.forClass(ItemRandomPool.class);
        Mockito.verify(shopRandomPoolRepository, Mockito.times(1))
            .savePool(Mockito.eq(personageId), Mockito.eq(itemType), captor.capture());

        // then
        final var expected = new ItemParamsFull(
            ItemRarity.LEGENDARY,
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
