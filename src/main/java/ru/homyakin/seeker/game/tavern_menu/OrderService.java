package ru.homyakin.seeker.game.tavern_menu;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemOrderError;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.models.OrderError;
import ru.homyakin.seeker.game.tavern_menu.models.OrderStatus;
import ru.homyakin.seeker.infrastructure.lock.LockPrefixes;
import ru.homyakin.seeker.infrastructure.lock.LockService;
import ru.homyakin.seeker.utils.TimeUtils;
import ru.homyakin.seeker.utils.models.Success;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);
    private static final Duration GIFT_TTL = Duration.of(5, ChronoUnit.MINUTES);
    private final PersonageService personageService;
    private final MenuItemOrderDao menuItemOrderDao;
    private final MenuService menuService;
    private final LockService lockService;

    public OrderService(
        PersonageService personageService,
        MenuItemOrderDao menuItemOrderDao,
        MenuService menuService,
        LockService lockService
    ) {
        this.personageService = personageService;
        this.menuItemOrderDao = menuItemOrderDao;
        this.menuService = menuService;
        this.lockService = lockService;
    }

    public Either<OrderError, Long> orderMenuItem(Personage giver, Personage acceptor, MenuItem menuItem) {
        if (!menuItem.isAvailable()) {
            return Either.left(new OrderError.NotAvailableItem());
        }
        if (giver.money().lessThan(menuItem.price())) {
            return Either.left(new OrderError.NotEnoughMoney(menuItem.category(), menuItem.price(), giver.money()));
        }
        //TODO проверку на то, что ещё ничего не заказано в этом чате
        personageService.takeMoney(giver, menuItem.price());
        return Either.right(
            menuItemOrderDao.createOrder(
                menuItem.id(),
                giver.id(),
                acceptor.id(),
                TimeUtils.moscowTime().plus(GIFT_TTL)
            )
        );
    }

    public Optional<MenuItemOrder> getById(long orderId) {
        return menuItemOrderDao.getById(orderId);
    }

    public Either<MenuItemOrderError, MenuItem> consume(long orderId, Personage consumer) {
        return lockService.tryLockAndCalc(
            lockOrderKey(orderId),
            () -> consumeLogic(orderId, consumer)
        ).fold(
            error -> Either.left(MenuItemOrderError.OrderLocked.INSTANCE),
            either -> either
        );
    }

    public void techCancelOrder(long orderId) {
        final var order = getById(orderId)
            .orElseThrow(() -> new IllegalStateException("No order to cancel; groupId=" + orderId));
        final var giver = personageService.getByIdForce(order.orderingPersonageId());
        final var menuItem = menuService.getMenuItem(order.menuItemId())
            .orElseThrow(() -> new IllegalStateException("Invalid menu item groupId=" + order.menuItemId()));
        personageService.addMoney(giver, menuItem.price());
        menuItemOrderDao.updateStatus(orderId, OrderStatus.TECH_CANCEL);
    }

    public Either<MenuItemOrderError.OrderLocked, Success> expireOrder(long orderId) {
        return lockService.tryLockAndExecute(
            lockOrderKey(orderId),
            () -> menuItemOrderDao.updateStatus(orderId, OrderStatus.EXPIRED)
        ).mapLeft(error -> MenuItemOrderError.OrderLocked.INSTANCE);
    }

    private Either<MenuItemOrderError, MenuItem> consumeLogic(long orderId, Personage consumer) {
        final var order = getById(orderId)
            .orElseThrow(() -> new IllegalStateException("Order " + orderId + " must present for consume"));
        if (order.status().isFinal()) {
            logger.error("Final status in consuming order: " + orderId);
            return Either.left(MenuItemOrderError.AlreadyFinalStatus.INSTANCE);
        }
        if (!order.acceptingPersonageId().equals(consumer.id())) {
            return Either.left(MenuItemOrderError.WrongConsumer.INSTANCE);
        }

        menuItemOrderDao.updateStatus(order.id(), OrderStatus.ACCEPTED);
        return Either.right(menuService.getMenuItem(order.menuItemId()).orElseThrow());
    }

    private String lockOrderKey(long orderId) {
        return LockPrefixes.MENU_ITEM_ORDER.name() + "-" + orderId;
    }
}
