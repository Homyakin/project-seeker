package ru.homyakin.seeker.game.tavern_menu;

import io.vavr.control.Either;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.tavern_menu.menu.MenuService;
import ru.homyakin.seeker.game.tavern_menu.menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.order.MenuItemOrderDao;
import ru.homyakin.seeker.game.tavern_menu.order.OrderConfig;
import ru.homyakin.seeker.game.tavern_menu.order.OrderService;
import ru.homyakin.seeker.game.tavern_menu.order.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.order.models.OrderStatus;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowOrderError;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowResult;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowTarget;
import ru.homyakin.seeker.infrastructure.lock.InMemoryLockService;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.test_utils.PersonageUtils;
import ru.homyakin.seeker.utils.TimeUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

public class OrderServiceThrowOrderTest {
    private final PersonageService personageService = Mockito.mock();
    private final MenuItemOrderDao menuItemOrderDao = Mockito.mock();
    private final MenuService menuService = Mockito.mock();
    private final LockService lockService = new InMemoryLockService();
    private final OrderConfig config = Mockito.mock();
    private final OrderService orderService = new OrderService(
        personageService,
        menuItemOrderDao,
        menuService,
        lockService,
        config
    );

    @BeforeEach
    public void init() {
        Mockito.when(config.throwEffectDuration()).thenReturn(Duration.ofMinutes(1));
        Mockito.when(config.throwCost()).thenReturn(Money.from(5));
        Mockito.when(config.throwDamageEffect()).thenReturn(new Effect.MinusMultiplier(1, EffectCharacteristic.HEALTH));
        Mockito.when(config.throwToStaffDamageEffect()).thenReturn(new Effect.MinusMultiplier(2, EffectCharacteristic.HEALTH));
    }

    @Test
    public void Given_EmptyOrders_Then_ReturnNoOrdersError() {
        final var orders = Collections.<MenuItemOrder>emptyList();
        final var personage = PersonageUtils.random();
        final var target = ThrowTarget.None.INSTANCE;
        final var groupId = new GroupId(1);
        Mockito.when(menuItemOrderDao.findNotFinalForPersonageInGroup(any(), any())).thenReturn(orders);

        final var result = orderService.throwOrder(personage, groupId, target);

        Assertions.assertEquals(ThrowOrderError.NoOrders.INSTANCE, result.getLeft());
    }

    @Test
    public void Given_CreatedOrder_Then_ReturnOnlyCreatedOrdersError() {
        // Given
        final var personage = PersonageUtils.random();
        final var order = new MenuItemOrder(
            1,
            1,
            PersonageId.from(1),
            personage.id(),
            LocalDateTime.now(),
            OrderStatus.CREATED
        );
        final var orders = List.of(order);
        final var target = ThrowTarget.None.INSTANCE;
        final var groupId = new GroupId(1);
        final MenuItem menuItem = Mockito.mock();
        Mockito.when(menuItem.category()).thenReturn(Category.DRINK);
        Mockito.when(menuService.getMenuItem(Mockito.anyInt())).thenReturn(Optional.of(menuItem));
        Mockito.when(menuItemOrderDao.findNotFinalForPersonageInGroup(any(), any())).thenReturn(orders);

        // When
        final var result = orderService.throwOrder(personage, groupId, target);

        // Then
        Assertions.assertEquals(new ThrowOrderError.OnlyCreatedOrders(menuItem.category()), result.getLeft());
    }

    @Test
    public void Given_ConsumedOrder_When_PersonageWithoutMoney_Then_ReturnNotEnoughMoneyError() {
        // Given
        final var personage = PersonageUtils.random();
        final var order = new MenuItemOrder(
            1,
            1,
            PersonageId.from(1),
            personage.id(),
            LocalDateTime.now(),
            OrderStatus.CONSUMED
        );
        final var orders = List.of(order);
        final var target = ThrowTarget.None.INSTANCE;
        final var groupId = new GroupId(1);
        final MenuItem menuItem = Mockito.mock();
        Mockito.when(menuItem.category()).thenReturn(Category.DRINK);
        Mockito.when(menuService.getMenuItem(Mockito.anyInt())).thenReturn(Optional.of(menuItem));
        Mockito.when(menuItemOrderDao.findNotFinalForPersonageInGroup(any(), any())).thenReturn(orders);

        // When
        final var result = orderService.throwOrder(personage, groupId, target);

        // Then
        Assertions.assertEquals(new ThrowOrderError.NotEnoughMoney(config.throwCost()), result.getLeft());
    }

    @Test
    public void Given_ConsumedOrder_When_ThrowToNone_Then_ReturnThrowToNoneResulAndTakeMoney() {
        // Given
        final var personage = personageWithMoney(config.throwCost());
        final var order = new MenuItemOrder(
            1,
            1,
            PersonageId.from(1),
            personage.id(),
            LocalDateTime.now(),
            OrderStatus.CONSUMED
        );
        final var orders = List.of(order);
        final var target = ThrowTarget.None.INSTANCE;
        final var groupId = new GroupId(1);
        final MenuItem menuItem = Mockito.mock();
        Mockito.when(menuItem.category()).thenReturn(Category.DRINK);
        Mockito.when(menuService.getMenuItem(Mockito.anyInt())).thenReturn(Optional.of(menuItem));
        Mockito.when(menuItemOrderDao.findNotFinalForPersonageInGroup(any(), any())).thenReturn(orders);

        // When
        final var result = orderService.throwOrder(personage, groupId, target);

        // Then
        Assertions.assertEquals(
            new ThrowResult.ThrowToNone(order.id(), config.throwCost(), Category.DRINK),
            result.get()
        );
        final var cost = config.throwCost();
        Mockito
            .verify(personageService, Mockito.times(1))
            .takeMoney(Mockito.eq(personage), Mockito.eq(cost));
    }

    @Test
    public void Given_ConsumedOrder_When_ThrowToOtherPersonage_Then_ReturnThrowToOtherPersonageResultAndTakeMoneyAndAddEffectToTarget() {
        // Given
        final var personage = personageWithMoney(config.throwCost());
        final var order = new MenuItemOrder(
            1,
            1,
            PersonageId.from(1),
            personage.id(),
            LocalDateTime.now(),
            OrderStatus.CONSUMED
        );
        final var orders = List.of(order);
        final var target = new ThrowTarget.PersonageTarget(PersonageUtils.random());
        final var groupId = new GroupId(1);
        final MenuItem menuItem = Mockito.mock();
        Mockito.when(menuItem.category()).thenReturn(Category.DRINK);
        Mockito.when(menuService.getMenuItem(Mockito.anyInt())).thenReturn(Optional.of(menuItem));
        Mockito.when(personageService.addEffect(any(), any(), any()))
            .thenReturn(target.personage());
        Mockito.when(menuItemOrderDao.findNotFinalForPersonageInGroup(any(), any())).thenReturn(orders);

        // When
        final var now = TimeUtils.moscowTime();
        final Either<ThrowOrderError, ThrowResult> result;
        try (final var mock = Mockito.mockStatic(TimeUtils.class)) {
            mock.when(TimeUtils::moscowTime).thenReturn(now);
            result = orderService.throwOrder(personage, groupId, target);
        }

        // Then
        Assertions.assertEquals(
            new ThrowResult.ThrowToOtherPersonage(
                order.id(), config.throwCost(), target.personage(), config.throwDamageEffect(), Category.DRINK
            ),
            result.get()
        );
        final var expected = new PersonageEffect(config.throwDamageEffect(), now.plus(config.throwEffectDuration()));
        Mockito
            .verify(personageService, Mockito.times(1))
            .addEffect(
                Mockito.eq(target.personage()),
                Mockito.eq(PersonageEffectType.THROW_DAMAGE_EFFECT),
                Mockito.eq(expected)
            );
    }

    @Test
    public void Given_ConsumedOrder_When_ThrowToSelfPersonage_Then_ReturnSelfThrowResultAndTakeMoneyAndAddEffectToTarget() {
        // Given
        final var personage = personageWithMoney(config.throwCost());
        final var order = new MenuItemOrder(
            1,
            1,
            PersonageId.from(1),
            personage.id(),
            LocalDateTime.now(),
            OrderStatus.CONSUMED
        );
        final var orders = List.of(order);
        final var target = new ThrowTarget.PersonageTarget(personage);
        final var groupId = new GroupId(1);
        final MenuItem menuItem = Mockito.mock();
        Mockito.when(menuItem.category()).thenReturn(Category.DRINK);
        Mockito.when(menuService.getMenuItem(Mockito.anyInt())).thenReturn(Optional.of(menuItem));
        Mockito.when(personageService.addEffect(any(), any(), any()))
            .thenReturn(target.personage());
        Mockito.when(menuItemOrderDao.findNotFinalForPersonageInGroup(any(), any())).thenReturn(orders);

        // When
        final var now = TimeUtils.moscowTime();
        final Either<ThrowOrderError, ThrowResult> result;
        try (final var mock = Mockito.mockStatic(TimeUtils.class)) {
            mock.when(TimeUtils::moscowTime).thenReturn(now);
            result = orderService.throwOrder(personage, groupId, target);
        }

        // Then
        Assertions.assertEquals(
            new ThrowResult.SelfThrow(
                order.id(), config.throwCost(), target.personage(), config.throwDamageEffect(), Category.DRINK
            ),
            result.get()
        );
        final var expected = new PersonageEffect(config.throwDamageEffect(), now.plus(config.throwEffectDuration()));
        Mockito
            .verify(personageService, Mockito.times(1))
            .addEffect(
                Mockito.eq(target.personage()),
                Mockito.eq(PersonageEffectType.THROW_DAMAGE_EFFECT),
                Mockito.eq(expected)
            );
    }

    @Test
    public void Given_ConsumedOrder_When_ThrowToStaff_Then_ReturnThrowToStaffResultAndTakeMoneyAndAddEffectToTarget() {
        // Given
        final var personage = personageWithMoney(config.throwCost());
        final var order = new MenuItemOrder(
            1,
            1,
            PersonageId.from(1),
            personage.id(),
            LocalDateTime.now(),
            OrderStatus.CONSUMED
        );
        final var orders = List.of(order);
        final var target = ThrowTarget.TavernStaff.INSTANCE;
        final var groupId = new GroupId(1);
        final MenuItem menuItem = Mockito.mock();
        Mockito.when(menuItem.category()).thenReturn(Category.DRINK);
        Mockito.when(menuService.getMenuItem(Mockito.anyInt())).thenReturn(Optional.of(menuItem));
        Mockito.when(personageService.addEffect(any(), any(), any()))
            .thenReturn(personage);
        Mockito.when(menuItemOrderDao.findNotFinalForPersonageInGroup(any(), any())).thenReturn(orders);

        // When
        final var now = TimeUtils.moscowTime();
        final Either<ThrowOrderError, ThrowResult> result;
        try (final var mock = Mockito.mockStatic(TimeUtils.class)) {
            mock.when(TimeUtils::moscowTime).thenReturn(now);
            result = orderService.throwOrder(personage, groupId, target);
        }

        // Then
        Assertions.assertEquals(
            new ThrowResult.ThrowToStaff(order.id(), config.throwCost(), config.throwToStaffDamageEffect(), Category.DRINK),
            result.get()
        );
        final var expected = new PersonageEffect(config.throwToStaffDamageEffect(), now.plus(config.throwEffectDuration()));
        Mockito
            .verify(personageService, Mockito.times(1))
            .addEffect(
                Mockito.eq(personage),
                Mockito.eq(PersonageEffectType.THROW_DAMAGE_EFFECT),
                Mockito.eq(expected)
            );
    }

    private Personage personageWithMoney(Money money) {
        return PersonageUtils.random().addMoney(money);
    }
}
