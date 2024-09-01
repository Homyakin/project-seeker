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
import ru.homyakin.seeker.game.tavern_menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemEffect;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemOrderError;
import ru.homyakin.seeker.game.tavern_menu.models.OrderStatus;
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
    private final MenuItemConfig config = Mockito.mock(MenuItemConfig.class);
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
        Either<MenuItemOrderError, MenuItem> result = orderService.consume(orderId, consumer);
        final var captor = ArgumentCaptor.forClass(MenuItemEffect.class);
        Mockito
            .verify(personageService, Mockito.times(1))
            .addMenuItemEffect(Mockito.eq(consumer), captor.capture());

        // then
        Assertions.assertTrue(result.isRight());
        Assertions.assertEquals(item, result.get());
        Assertions.assertEquals(item.effect(), captor.getValue().effect());
        Mockito.verify(menuItemOrderDao, Mockito.times(1)).updateStatus(orderId, OrderStatus.ACCEPTED);
    }

    @Test
    public void Given_NonExistOrder_When_Consume_Then_ThrowException() {
        // Arrange
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
            OrderStatus.ACCEPTED
        );
        Mockito.when(menuItemOrderDao.getById(orderId)).thenReturn(Optional.of(order));

        // when
        Either<MenuItemOrderError, MenuItem> result = orderService.consume(orderId, consumer);

        // then
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(MenuItemOrderError.WrongConsumer.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_OrderIsFinalStatus_When_Consume_Then_ReturnAlreadyFinalStatus() {
        // Arrange
        long orderId = 1L;
        final var consumer = PersonageUtils.random();
        final var order = new MenuItemOrder(
            orderId,
            item.id(),
            PersonageId.from(1),
            consumer.id(),
            TimeUtils.moscowTime(),
            OrderStatus.ACCEPTED
        );

        Mockito.when(menuItemOrderDao.getById(orderId)).thenReturn(Optional.of(order));

        // Act
        Either<MenuItemOrderError, MenuItem> result = orderService.consume(orderId, consumer);

        // Assert
        Assertions.assertTrue(result.isLeft());
        Assertions.assertEquals(MenuItemOrderError.AlreadyFinalStatus.INSTANCE, result.getLeft());
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
