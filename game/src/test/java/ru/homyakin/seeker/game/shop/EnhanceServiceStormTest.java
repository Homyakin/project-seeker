package ru.homyakin.seeker.game.shop;

import io.vavr.control.Either;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.PersonageItem;
import ru.homyakin.seeker.game.item.storm.StormEnhanceConfig;
import ru.homyakin.seeker.game.item.storm.StormEnhanceOutcomePicker;
import ru.homyakin.seeker.game.item.storm.StormEnhanceTechnicalLimit;
import ru.homyakin.seeker.game.models.StormShards;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughStormShards;
import ru.homyakin.seeker.game.shop.errors.StormEnhanceError;
import ru.homyakin.seeker.game.shop.models.StormEnhanceOutcome;
import ru.homyakin.seeker.utils.models.Success;

class EnhanceServiceStormTest {
    private static final PersonageId PERSONAGE_ID = PersonageId.from(1);
    private static final long ITEM_ID = 2;

    private final ItemService itemService = Mockito.mock(ItemService.class);
    private final PersonageService personageService = Mockito.mock(PersonageService.class);
    private final StormEnhanceOutcomePicker outcomePicker = Mockito.mock(StormEnhanceOutcomePicker.class);
    private final StormEnhanceConfig stormConfig = new StormEnhanceConfig();
    private final EnhanceService service = new EnhanceService(
        itemService,
        personageService,
        new ShopConfig(),
        stormConfig,
        new StormEnhanceTechnicalLimit(stormConfig),
        outcomePicker
    );

    @BeforeEach
    void setUp() {
        Mockito.when(personageService.tryTakeStormShards(Mockito.eq(PERSONAGE_ID), Mockito.any()))
            .thenReturn(Either.right(Success.INSTANCE));
    }

    @Test
    void stormEnhance_isTransactional() throws NoSuchMethodException {
        final var method = EnhanceService.class.getMethod(
            "stormEnhance",
            PersonageId.class,
            long.class,
            int.class,
            long.class
        );

        Assertions.assertNotNull(method.getAnnotation(Transactional.class));
    }

    @Test
    void success_usesLockedStateDebitsAndAdvancesLevelAndRevision() {
        final var current = item(0, 4);
        final var updated = item(1, 5);
        Mockito.when(itemService.getPersonageItemForUpdate(PERSONAGE_ID, ITEM_ID))
            .thenReturn(Optional.of(current));
        Mockito.when(outcomePicker.pick(Mockito.any())).thenReturn(StormEnhanceOutcome.SUCCESS);
        Mockito.when(itemService.applyStormEnhance(current, 1)).thenReturn(updated);

        final var result = service.stormEnhance(PERSONAGE_ID, ITEM_ID, 0, 4);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(StormEnhanceOutcome.SUCCESS, result.get().outcome());
        Assertions.assertEquals(1, result.get().action().item().enhanceLevel());
        Assertions.assertEquals(5, result.get().action().item().enhanceRevision());
        final var order = Mockito.inOrder(personageService, itemService, outcomePicker);
        order.verify(personageService).lockForItemChange(PERSONAGE_ID);
        order.verify(itemService).getPersonageItemForUpdate(PERSONAGE_ID, ITEM_ID);
        order.verify(personageService).tryTakeStormShards(PERSONAGE_ID, StormShards.from(25));
        order.verify(outcomePicker).pick(Mockito.any());
        order.verify(itemService).applyStormEnhance(current, 1);
    }

    @Test
    void failure_keepsLevelButAdvancesRevision() {
        final var current = item(6, 10);
        final var updated = item(6, 11);
        Mockito.when(itemService.getPersonageItemForUpdate(PERSONAGE_ID, ITEM_ID))
            .thenReturn(Optional.of(current));
        Mockito.when(outcomePicker.pick(Mockito.any())).thenReturn(StormEnhanceOutcome.FAILURE);
        Mockito.when(itemService.applyStormEnhance(current, 6)).thenReturn(updated);

        final var result = service.stormEnhance(PERSONAGE_ID, ITEM_ID, 6, 10);

        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(StormEnhanceOutcome.FAILURE, result.get().outcome());
        Assertions.assertEquals(6, result.get().action().item().enhanceLevel());
        Assertions.assertEquals(11, result.get().action().item().enhanceRevision());
        Mockito.verify(itemService).applyStormEnhance(current, 6);
    }

    @Test
    void staleLevelOrRevision_isRejectedBeforeDebit() {
        final var current = item(3, 8);
        Mockito.when(itemService.getPersonageItemForUpdate(PERSONAGE_ID, ITEM_ID))
            .thenReturn(Optional.of(current));

        final var staleLevel = service.stormEnhance(PERSONAGE_ID, ITEM_ID, 2, 8);
        final var staleRevision = service.stormEnhance(PERSONAGE_ID, ITEM_ID, 3, 7);

        Assertions.assertInstanceOf(StormEnhanceError.StaleItemState.class, staleLevel.getLeft());
        Assertions.assertInstanceOf(StormEnhanceError.StaleItemState.class, staleRevision.getLeft());
        Mockito.verify(personageService, Mockito.never()).tryTakeStormShards(Mockito.any(), Mockito.any());
        Mockito.verifyNoInteractions(outcomePicker);
        Mockito.verify(itemService, Mockito.never()).applyStormEnhance(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void insufficientShards_doesNotPickOutcomeOrChangeItem() {
        final var current = item(0, 0);
        Mockito.when(itemService.getPersonageItemForUpdate(PERSONAGE_ID, ITEM_ID))
            .thenReturn(Optional.of(current));
        Mockito.when(personageService.tryTakeStormShards(PERSONAGE_ID, StormShards.from(25)))
            .thenReturn(Either.left(new NotEnoughStormShards(StormShards.from(25))));

        final var result = service.stormEnhance(PERSONAGE_ID, ITEM_ID, 0, 0);

        Assertions.assertInstanceOf(StormEnhanceError.NotEnoughStormShards.class, result.getLeft());
        Mockito.verifyNoInteractions(outcomePicker);
        Mockito.verify(itemService, Mockito.never()).applyStormEnhance(Mockito.any(), Mockito.anyInt());
    }

    @Test
    void technicalLimit_hasNoActionAndRejectsAttemptBeforeDebit() {
        final var current = item(stormConfig.maxPriceSupportedStateLevel(), 20);
        Mockito.when(itemService.getPersonageItem(PERSONAGE_ID, ITEM_ID)).thenReturn(Optional.of(current));
        Mockito.when(itemService.getPersonageItemForUpdate(PERSONAGE_ID, ITEM_ID))
            .thenReturn(Optional.of(current));

        final var action = service.availableAction(PERSONAGE_ID, ITEM_ID).get();
        final var result = service.stormEnhance(
            PERSONAGE_ID,
            ITEM_ID,
            current.enhanceLevel(),
            current.enhanceRevision()
        );

        Assertions.assertTrue(action.stormEnhance().isEmpty());
        Assertions.assertInstanceOf(StormEnhanceError.TechnicalLimitReached.class, result.getLeft());
        Mockito.verify(personageService, Mockito.never()).tryTakeStormShards(Mockito.any(), Mockito.any());
    }

    private static PersonageItem item(int level, long revision) {
        final var object = new ItemObject(
            "body",
            Set.of(PersonageSlot.BODY),
            Optional.empty(),
            Optional.empty(),
            60,
            0,
            0,
            0,
            0,
            0,
            Map.of()
        );
        return new PersonageItem(
            ITEM_ID,
            1,
            object,
            Optional.empty(),
            Optional.empty(),
            ItemRarity.LEGENDARY,
            Optional.of(PERSONAGE_ID),
            false,
            level,
            revision
        );
    }
}
