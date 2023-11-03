package ru.homyakin.seeker.game.tavern_menu;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.tavern_menu.models.ConsumeError;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemOrder;
import ru.homyakin.seeker.game.tavern_menu.models.OrderError;
import ru.homyakin.seeker.game.tavern_menu.models.OrderStatus;
import ru.homyakin.seeker.utils.TimeUtils;

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

    public OrderService(
        PersonageService personageService,
        MenuItemOrderDao menuItemOrderDao,
        MenuService menuService
    ) {
        this.personageService = personageService;
        this.menuItemOrderDao = menuItemOrderDao;
        this.menuService = menuService;
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

    public Either<ConsumeError, MenuItem> consume(long orderId, Personage consumer) {
        final var order = getById(orderId)
            .orElseThrow(() -> new IllegalStateException("Order with id " + orderId + " must be for consume"));
        if (order.status().isFinal()) {
            logger.error("Final status in consuming order: " + orderId);
            return Either.left(ConsumeError.AlreadyFinalStatus.INSTANCE);
        }
        if (!order.acceptingPersonageId().equals(consumer.id())) {
            return Either.left(ConsumeError.WrongConsumer.INSTANCE);
        }

        menuItemOrderDao.updateStatus(order.id(), OrderStatus.ACCEPTED);
        return Either.right(menuService.getMenuItem(order.menuItemId()).orElseThrow());
    }

    public void techCancelOrder(long orderId) {
        final var order = getById(orderId)
            .orElseThrow(() -> new IllegalStateException("No order to cancel; id=" + orderId));
        final var giver = personageService.getByIdForce(order.orderingPersonageId());
        final var menuItem = menuService.getMenuItem(order.menuItemId())
            .orElseThrow(() -> new IllegalStateException("Invalid menu item id=" + order.menuItemId()));
        personageService.addMoney(giver, menuItem.price());
        menuItemOrderDao.updateStatus(orderId, OrderStatus.TECH_CANCEL);
    }

    public void expireOrder(long orderId) {
        menuItemOrderDao.updateStatus(orderId, OrderStatus.EXPIRED);
    }
}
