package ru.homyakin.seeker.game.tavern_menu.order;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffect;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType;
import ru.homyakin.seeker.game.tavern_menu.menu.MenuService;
import ru.homyakin.seeker.game.tavern_menu.menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.order.models.ConsumeResult;
import ru.homyakin.seeker.game.tavern_menu.order.models.ExpireOrderError;
import ru.homyakin.seeker.game.tavern_menu.order.models.ExpiredOrder;
import ru.homyakin.seeker.game.tavern_menu.order.models.ConsumeOrderError;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.order.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.order.models.OrderError;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowOrderError;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowResult;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowTarget;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.TimeUtils;

import java.util.Optional;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private final PersonageService personageService;
    private final MenuItemOrderDao menuItemOrderDao;
    private final MenuService menuService;
    private final LockService lockService;
    private final OrderConfig config;

    public OrderService(
        PersonageService personageService,
        MenuItemOrderDao menuItemOrderDao,
        MenuService menuService,
        LockService lockService,
        OrderConfig config
    ) {
        this.personageService = personageService;
        this.menuItemOrderDao = menuItemOrderDao;
        this.menuService = menuService;
        this.lockService = lockService;
        this.config = config;
    }

    public Either<OrderError, Long> orderMenuItem(
        Personage giver,
        Personage acceptor,
        GroupId groupId,
        MenuItem menuItem
    ) {
        if (!menuItem.isAvailable()) {
            return Either.left(new OrderError.NotAvailableItem());
        }
        if (giver.money().lessThan(menuItem.price())) {
            return Either.left(new OrderError.NotEnoughMoney(menuItem.category(), menuItem.price(), giver.money()));
        }

        personageService.takeMoney(giver, menuItem.price());
        return Either.right(
            menuItemOrderDao.createOrder(
                menuItem.id(),
                giver.id(),
                acceptor.id(),
                groupId,
                TimeUtils.moscowTime().plus(config.orderTtl())
            )
        );
    }

    public Optional<MenuItemOrder> getById(long orderId) {
        return menuItemOrderDao.getById(orderId);
    }

    @Transactional
    public Either<ConsumeOrderError, ConsumeResult> consume(long orderId, Personage consumer) {
        return lockService.tryLockAndCalc(
            lockOrderKey(orderId),
            () -> consumeLogic(orderId, consumer)
        ).fold(
            _ -> Either.left(ConsumeOrderError.OrderLocked.INSTANCE),
            either -> either
        );
    }

    @Transactional
    public void techCancelOrder(long orderId) {
        final var order = getById(orderId)
            .orElseThrow(() -> new IllegalStateException("No order to cancel; id=" + orderId));
        final var giver = personageService.getByIdForce(order.orderingPersonageId());
        final var menuItem = menuService.getMenuItem(order.menuItemId())
            .orElseThrow(() -> new IllegalStateException("Invalid menu item id=" + order.menuItemId()));
        personageService.addMoney(giver, menuItem.price());
        menuItemOrderDao.update(order.techCancel());
    }

    public Either<ExpireOrderError, ExpiredOrder> expireOrder(long orderId) {
        return lockService.tryLockAndCalc(
            lockOrderKey(orderId),
            () -> expireOrderLogic(orderId)
        ).fold(
            _ -> Either.left(ExpireOrderError.OrderLocked.INSTANCE),
            either -> either
        );
    }

    public Either<ThrowOrderError, ThrowResult> throwOrder(Personage personage, GroupId groupId, ThrowTarget target) {
        final var findResult = findOrderToThrow(personage.id(), groupId);
        if (findResult.isLeft()) {
            return Either.left(findResult.getLeft());
        }
        final var orderToThrow = findResult.get();

        return lockService.tryLockAndCalc(
            lockOrderKey(orderToThrow.id()),
            () -> throwOrderLogic(orderToThrow, personage, target)
        ).fold(
            _ -> Either.left(ThrowOrderError.OrderLocked.INSTANCE),
            either -> either
        );
    }

    @Transactional
    public Either<ThrowOrderError, ThrowResult.ThrowToOtherPersonage> throwOrder(
        Personage throwing,
        GroupId groupId,
        GroupId targetGroupId,
        Personage target
    ) {
        final var findResult = findOrderToThrow(throwing.id(), groupId);
        if (findResult.isLeft()) {
            return Either.left(findResult.getLeft());
        }
        final var orderToThrow = findResult.get();

        return lockService.tryLockAndCalc(
            lockOrderKey(orderToThrow.id()),
            () -> throwOrderLogic(orderToThrow, throwing, targetGroupId, target)
        ).fold(
            _ -> Either.left(ThrowOrderError.OrderLocked.INSTANCE),
            either -> either
        );
    }

    private Either<ThrowOrderError, ThrowResult.ThrowToOtherPersonage> throwOrderLogic(
        MenuItemOrder order,
        Personage throwing,
        GroupId targetGroupId,
        Personage target
    ) {
        final var cost = config.throwCost();
        final var category = menuService.getMenuItem(order.menuItemId()).orElseThrow().category();
        if (throwing.money().lessThan(cost)) {
            return Either.left(new ThrowOrderError.NotEnoughMoney(cost));
        }

        personageService.takeMoney(throwing, cost);

        final var result = throwToPersonage(
            order.id(),
            throwing,
            target,
            category
        );

        if (result instanceof ThrowResult.ThrowToOtherPersonage throwToOtherPersonage) {
            menuItemOrderDao.updateThrowTargets(order.id(), target.id(), targetGroupId);
            return Either.right(throwToOtherPersonage);
        }
        logger.error("Unexpected throw result {}", result);
        throw new IllegalStateException("Unexpected throw result " + result);
    }

    private Either<ThrowOrderError, MenuItemOrder> findOrderToThrow(PersonageId personageId, GroupId groupId) {
        final var orders = menuItemOrderDao.findNotFinalForPersonageInGroup(personageId, groupId);
        if (orders.isEmpty()) {
            return Either.left(ThrowOrderError.NoOrders.INSTANCE);
        }
        final var orderToThrow = orders
            .stream()
            .filter(MenuItemOrder::canThrow)
            .findFirst();
        if (orderToThrow.isEmpty()) {
            final var category = menuService.getMenuItem(orders.getFirst().menuItemId()).orElseThrow().category();
            return Either.left(new ThrowOrderError.OnlyCreatedOrders(category));
        }
        return Either.right(orderToThrow.get());
    }

    private Either<ThrowOrderError, ThrowResult> throwOrderLogic(MenuItemOrder order, Personage throwing, ThrowTarget target) {
        final var cost = config.throwCost();
        final var category = menuService.getMenuItem(order.menuItemId()).orElseThrow().category();
        if (throwing.money().lessThan(cost)) {
            return Either.left(new ThrowOrderError.NotEnoughMoney(cost));
        }

        personageService.takeMoney(throwing, cost);

        final var result = switch (target) {
            case ThrowTarget.None _ -> new ThrowResult.ThrowToNone(order.id(), cost, category);
            case ThrowTarget.PersonageTarget personageTarget ->
                throwToPersonage(
                    order.id(),
                    throwing,
                    personageTarget.personage(),
                    category
                );
            case ThrowTarget.TavernStaff _ -> {
                final var effect = new PersonageEffect(
                    config.throwToStaffDamageEffect(),
                    TimeUtils.moscowTime().plus(config.throwEffectDuration())
                );
                personageService.addEffect(throwing, PersonageEffectType.THROW_DAMAGE_EFFECT, effect);
                yield new ThrowResult.ThrowToStaff(order.id(), cost, effect.effect(), category);
            }
        };

        menuItemOrderDao.update(result);

        return Either.right(result);
    }

    private ThrowResult throwToPersonage(
        long orderId,
        Personage throwing,
        Personage targetPersonage,
        Category category
    ) {
        final var cost = config.throwCost();
        final var effect = new PersonageEffect(
            config.throwDamageEffect(),
            TimeUtils.moscowTime().plus(config.throwEffectDuration())
        );
        final var updatedTarget = personageService.addEffect(
            targetPersonage,
            PersonageEffectType.THROW_DAMAGE_EFFECT,
            effect
        );
        if (targetPersonage.id().equals(throwing.id())) {
            return new ThrowResult.SelfThrow(orderId, cost, updatedTarget, effect.effect(), category);
        } else {
            return new ThrowResult.ThrowToOtherPersonage(orderId, cost, updatedTarget, effect.effect(), category);
        }
    }

    private Either<ConsumeOrderError, ConsumeResult> consumeLogic(long orderId, Personage consumer) {
        final var consumeResult = getById(orderId)
            .orElseThrow(() -> new IllegalStateException("Order " + orderId + " must present for consume"))
            .consume(consumer, TimeUtils.moscowTime(), config.timeToThrowOrder());
        if (consumeResult.isLeft()) {
            return Either.left(consumeResult.getLeft());
        }
        final var order = consumeResult.get();
        final var item = menuService.getMenuItem(order.menuItemId()).orElseThrow();
        menuItemOrderDao.update(order);
        final var effect = new PersonageEffect(item.effect(), TimeUtils.moscowTime().plus(config.effectDuration()));
        final var updatedPersonage = personageService.addEffect(consumer, PersonageEffectType.MENU_ITEM_EFFECT, effect);
        return Either.right(new ConsumeResult(item, effect, updatedPersonage));
    }

    private Either<ExpireOrderError, ExpiredOrder> expireOrderLogic(long orderId) {
        return menuItemOrderDao.getById(orderId)
            .orElseThrow()
            .expire()
            .peek(menuItemOrderDao::update);
    }

    private String lockOrderKey(long orderId) {
        return LockPrefixes.MENU_ITEM_ORDER.name() + "-" + orderId;
    }
}
