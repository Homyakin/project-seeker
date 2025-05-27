package ru.homyakin.seeker.telegram.group.taver_menu;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.tavern_menu.order.models.ExpiredOrder;
import ru.homyakin.seeker.game.tavern_menu.order.models.OrderError;
import ru.homyakin.seeker.game.tavern_menu.order.OrderService;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowResult;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowTarget;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.game.stats.action.GroupStatsService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.TimeUtils;

import java.util.Optional;

@Service
public class OrderTgService {
    private static final Logger logger = LoggerFactory.getLogger(OrderTgService.class);
    private final MenuItemOrderTgDao menuItemOrderTgDao;
    private final OrderService orderService;
    private final GroupStatsService groupStatsService;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;
    private final GroupTgService groupTgService;
    private final UserService userService;

    public OrderTgService(
        MenuItemOrderTgDao menuItemOrderTgDao,
        OrderService orderService,
        GroupStatsService groupStatsService,
        TelegramSender telegramSender,
        PersonageService personageService,
        GroupTgService groupTgService,
        UserService userService
    ) {
        this.menuItemOrderTgDao = menuItemOrderTgDao;
        this.orderService = orderService;
        this.groupStatsService = groupStatsService;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
        this.groupTgService = groupTgService;
        this.userService = userService;
    }

    public Either<OrderError, MenuItemOrderTg> orderMenuItem(GroupTg group, User giver, User acceptor, MenuItem menuItem) {
        final var acceptingPersonage = personageService.getByIdForce(acceptor.personageId());
        final Personage givingPersonage;
        if (giver == acceptor) {
            givingPersonage = acceptingPersonage;
        } else {
            givingPersonage = personageService.getByIdForce(giver.personageId());
        }
        return orderService.orderMenuItem(givingPersonage, acceptingPersonage, group.domainGroupId(), menuItem)
            .map(orderId -> {
                    groupStatsService.increaseTavernMoneySpent(group.domainGroupId(), givingPersonage.id(), menuItem.price());
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
                            orderService.techCancelOrder(orderId);
                            return new IllegalStateException("Exception during sending telegram message");
                        });
                }
            );
    }

    public Either<ThrowOrderTgError, ThrowResultTg> throwOrder(
        User throwing,
        Optional<MentionInfo> target,
        GroupTg groupTg
    ) {
        final ThrowTarget throwTarget;
        if (target.isEmpty()) {
            throwTarget = ThrowTarget.None.INSTANCE;
        } else {
            final Optional<ThrowTarget> optional = switch (target.get().userType()) {
                case USER -> {
                    final var user = userService.getByMention(target.get(), groupTg.id());
                    if (user.isEmpty()) {
                        yield Optional.empty();
                    }
                    yield Optional.of(
                        new ThrowTarget.PersonageTarget(personageService.getByIdForce(user.get().personageId()))
                    );
                }
                case THIS_BOT -> Optional.of(ThrowTarget.TavernStaff.INSTANCE);
                case DIFFERENT_BOT -> Optional.of(ThrowTarget.None.INSTANCE);
            };
            if (optional.isEmpty()) {
                return Either.left(ThrowOrderTgError.UserNotFound.INSTANCE);
            }
            throwTarget = optional.get();
        }
        final var personage = personageService.getByIdForce(throwing.personageId());

        return orderService.throwOrder(personage, groupTg.domainGroupId(), throwTarget)
            .<ThrowOrderTgError>mapLeft(ThrowOrderTgError.Domain::new)
            .map(this::mapThrowResultToTg);
    }

    private ThrowResultTg mapThrowResultToTg(ThrowResult throwResult) {
        return switch (throwResult) {
            case ThrowResult.SelfThrow selfThrow -> new ThrowResultTg.SelfThrow(selfThrow);
            case ThrowResult.ThrowToNone throwToNone -> new ThrowResultTg.ThrowToNone(throwToNone);
            case ThrowResult.ThrowToOtherPersonage throwToOtherPersonage -> new ThrowResultTg.ThrowToOtherPersonage(
                throwToOtherPersonage.cost(),
                TgPersonageMention.of(
                    throwToOtherPersonage.personage(),
                    userService.getByPersonageIdForce(throwToOtherPersonage.personage().id()).id()
                ),
                throwToOtherPersonage.effect(),
                throwToOtherPersonage.category()
            );
            case ThrowResult.ThrowToStaff throwToStaff -> new ThrowResultTg.ThrowToStaff(throwToStaff);
        };
    }

    @Scheduled(cron = "0 * * * * *")
    public void expireOldOrders() {
        logger.debug("Expiring orders");
        menuItemOrderTgDao.findNotFinalWithLessExpireDateTime(TimeUtils.moscowTime()).forEach(
            order -> {
                logger.info("Order " + order.menuItemOrderId() + " expired");
                orderService.expireOrder(order.menuItemOrderId())
                    .peek(expiredOrder -> {
                        if (expiredOrder.status() == ExpiredOrder.Status.EXPIRED) {
                            final var group = groupTgService.getOrCreate(order.groupTgId());
                            telegramSender.send(
                                EditMessageTextBuilder.builder()
                                    .text(TavernMenuLocalization.expiredOrder(group.language()))
                                    .messageId(order.messageId())
                                    .chatId(order.groupTgId())
                                    .build()
                            );
                        }
                    });
            }
        );
    }

    private MenuItemOrderTg linkOrderToMessage(long orderId, GroupTgId groupId, int messageId) {
        return menuItemOrderTgDao.insert(
            new MenuItemOrderTg(
                orderId,
                groupId,
                messageId
            )
        );
    }
}
