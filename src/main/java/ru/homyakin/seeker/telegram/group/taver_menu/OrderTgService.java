package ru.homyakin.seeker.telegram.group.taver_menu;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.tavern_menu.models.OrderError;
import ru.homyakin.seeker.game.tavern_menu.OrderService;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupService;
import ru.homyakin.seeker.telegram.group.GroupStatsService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.MenuItemOrderTg;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.TimeUtils;

@Service
public class OrderTgService {
    private static final Logger logger = LoggerFactory.getLogger(OrderTgService.class);
    private final MenuItemOrderTgDao menuItemOrderTgDao;
    private final OrderService orderService;
    private final GroupStatsService groupStatsService;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;
    private final GroupService groupService;

    public OrderTgService(
        MenuItemOrderTgDao menuItemOrderTgDao,
        OrderService orderService,
        GroupStatsService groupStatsService,
        TelegramSender telegramSender,
        PersonageService personageService,
        GroupService groupService
    ) {
        this.menuItemOrderTgDao = menuItemOrderTgDao;
        this.orderService = orderService;
        this.groupStatsService = groupStatsService;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
        this.groupService = groupService;
    }

    public Either<OrderError, MenuItemOrderTg> orderMenuItem(Group group, User giver, User acceptor, MenuItem menuItem) {
        final var acceptingPersonage = personageService.getByIdForce(acceptor.personageId());
        final Personage givingPersonage;
        if (giver == acceptor) {
            givingPersonage = acceptingPersonage;
        } else {
            givingPersonage = personageService.getByIdForce(giver.personageId());
        }
        return orderService.orderMenuItem(givingPersonage, acceptingPersonage, menuItem)
            .map(orderId -> {
                    groupStatsService.increaseTavernMoneySpent(group.id(), menuItem.price());
                    return telegramSender.send(SendMessageBuilder.builder()
                            .chatId(group.id())
                            .text(
                                TavernMenuLocalization.order(
                                    group.language(),
                                    menuItem,
                                    TgPersonageMention.of(givingPersonage, giver.id()),
                                    TgPersonageMention.of(acceptingPersonage, acceptor.id())
                                )
                            )
                            .keyboard(InlineKeyboards.consumeMenuItemOrderKeyboard(group.language(), orderId, menuItem))
                            .build()
                        )
                        .map(message -> linkOrderToMessage(orderId, group.id(), message.getMessageId()))
                        .getOrElseThrow(() -> {
                            groupStatsService.increaseTavernMoneySpent(group.id(), menuItem.price().negative());
                            orderService.techCancelOrder(orderId);
                            return new IllegalStateException("Exception during sending telegram message");
                        });
                }
            );
    }

    @Scheduled(cron = "0 * * * * *")
    public void expireOldOrders() {
        logger.debug("Expiring orders");
        menuItemOrderTgDao.findNotFinalWithLessExpireDateTime(TimeUtils.moscowTime()).forEach(
            order -> {
                logger.info("Order " + order.menuItemOrderId() + " was expired");
                orderService.expireOrder(order.menuItemOrderId());
                final var group = groupService.getOrCreate(order.groupTgId());
                telegramSender.send(
                    EditMessageTextBuilder.builder()
                        .text(TavernMenuLocalization.expiredOrder(group.language()))
                        .messageId(order.messageId())
                        .chatId(order.groupTgId())
                        .build()
                );
            }
        );
    }

    private MenuItemOrderTg linkOrderToMessage(long orderId, GroupId groupId, int messageId) {
        return menuItemOrderTgDao.insert(
            new MenuItemOrderTg(
                orderId,
                groupId,
                messageId
            )
        );
    }
}
