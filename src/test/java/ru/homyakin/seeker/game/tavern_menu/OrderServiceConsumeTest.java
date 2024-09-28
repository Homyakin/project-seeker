package ru.homyakin.seeker.game.tavern_menu;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.tavern_menu.menu.MenuService;
import ru.homyakin.seeker.game.tavern_menu.menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.order.OrderConfig;
import ru.homyakin.seeker.game.tavern_menu.order.models.ConsumeResult;
import ru.homyakin.seeker.game.tavern_menu.order.models.MenuItemEffect;
import ru.homyakin.seeker.game.tavern_menu.order.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.order.models.ConsumeOrderError;
import ru.homyakin.seeker.game.tavern_menu.order.models.OrderStatus;
import ru.homyakin.seeker.game.tavern_menu.order.MenuItemOrderDao;
import ru.homyakin.seeker.game.tavern_menu.order.OrderService;
import ru.homyakin.seeker.infrastructure.lock.InMemoryLockService;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

public class OrderServiceConsumeTest {
    private final PersonageService personageService = Mockito.mock(PersonageService.class);
    private final MenuItemOrderDao menuItemOrderDao = Mockito.mock(MenuItemOrderDao.class);
    private final MenuService menuService = Mockito.mock(MenuService.class);
    private final LockService lockService = new InMemoryLockService();
    private final OrderConfig config = Mockito.mock(OrderConfig.class);
    private final OrderService orderService = new OrderService(
        personageService,
        menuItemOrderDao,
        menuService,
        lockService,
        config
    );

    @BeforeEach
    public void init() {
        Mockito.when(config.effectDuration()).thenReturn(Duration.ofMinutes(1));
        Mockito.when(config.timeToThrowOrder()).thenReturn(Duration.ofMinutes(1));
    }

    @Test
    public void Given_ConsumerEqualsOrderAcceptor_And_OrderStatusIsCreated_When_Consume_Then_UpdateOrderStatusToAccepted_And_AddEffect() {
        // given
        long orderId = 1L;
        final var consumer = PersonageUtils.random();
        final var order = new MenuItemOrder(
            orderId,
            item.id(),
            PersonageId.from(1),
            consumer.id(),
            TimeUtils.moscowTime(),
            OrderStatus.CREATED
        );

        Mockito.when(menuItemOrderDao.getById(orderId)).thenReturn(Optional.of(order));
        Mockito.when(menuService.getMenuItem(order.menuItemId())).thenReturn(Optional.of(item));

        // when
        final Either<ConsumeOrderError, ConsumeResult> result;
        final var now = TimeUtils.moscowTime();
        try (final var mock = Mockito.mockStatic(TimeUtils.class)) {
            mock.when(TimeUtils::moscowTime).thenReturn(now);
            result = orderService.consume(orderId, consumer);
        }
        final var type = ArgumentCaptor.forClass(PersonageEffectType.class);
        final var effect = ArgumentCaptor.forClass(PersonageEffect.class);
        Mockito
            .verify(personageService, Mockito.times(1))
            .addEffect(Mockito.eq(consumer), type.capture(), effect.capture());

        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(item, result.get().item());
        Assertions.assertEquals(PersonageEffectType.MENU_ITEM_EFFECT, type.getValue());
        Assertions.assertEquals(item.effect(), effect.getValue().effect());

        final var expected = new MenuItemOrder(
            order.id(),
            order.menuItemId(),
            order.orderingPersonageId(),
            order.acceptingPersonageId(),
            now.plus(config.timeToThrowOrder()),
            OrderStatus.CONSUMED
        );
        Mockito.verify(menuItemOrderDao, Mockito.times(1)).update(expected);
    }

    @Test
    public void Given_NonExistOrder_When_Consume_Then_ThrowException() {
        // Given
        long orderId = 1L;
        final var consumer = PersonageUtils.random();

        Mockito.when(menuItemOrderDao.getById(orderId)).thenReturn(Optional.empty());

        // when
        Assertions.assertThrows(
            IllegalStateException.class,
            () -> orderService.consume(orderId, consumer)
        );
    }

    @Test
    public void Given_ConsumerNotEqualsOrderAcceptor_And_OrderIsAccepted_When_Consume_Then_ReturnWrongConsumer() {
        // given
        long orderId = 1L;
        final var consumer = PersonageUtils.withId(PersonageId.from(1));
        final var order = new MenuItemOrder(
            orderId,
            item.id(),
            PersonageId.from(1),
            PersonageId.from(2),
            TimeUtils.moscowTime(),
            OrderStatus.CONSUMED
        );
        Mockito.when(menuItemOrderDao.getById(orderId)).thenReturn(Optional.of(order));

        // when
        final var result = orderService.consume(orderId, consumer);

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(ConsumeOrderError.WrongConsumer.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_OrderIsFinalStatus_When_Consume_Then_ReturnAlreadyFinalStatus() {
        // Given
        long orderId = 1L;
        final var consumer = PersonageUtils.random();
        final var order = new MenuItemOrder(
            orderId,
            item.id(),
            PersonageId.from(1),
            consumer.id(),
            TimeUtils.moscowTime(),
            OrderStatus.CONSUMED
        );

        Mockito.when(menuItemOrderDao.getById(orderId)).thenReturn(Optional.of(order));

        // When
        final var result = orderService.consume(orderId, consumer);

        // Then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(ConsumeOrderError.AlreadyFinalStatus.INSTANCE, result.getLeft());
    }

    private final MenuItem item = new MenuItem(
        1,
        "name",
        Money.from(1),
        true,
        Category.DRINK,
        Collections.emptyMap(),
        new Effect.Multiplier(1, EffectCharacteristic.HEALTH)
    );
}
