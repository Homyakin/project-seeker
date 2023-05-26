package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.tavern_menu.MenuService;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.taver_menu.OrderTgService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.models.MessageOwner;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.utils.models.Failure;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class OrderExecutor extends CommandExecutor<Order> {
    private final GroupUserService groupUserService;
    private final MenuService menuService;
    private final TelegramSender telegramSender;
    private final UserService userService;
    private final OrderTgService orderTgService;

    public OrderExecutor(
        GroupUserService groupUserService,
        MenuService menuService,
        TelegramSender telegramSender,
        UserService userService,
        OrderTgService orderTgService
    ) {
        this.groupUserService = groupUserService;
        this.menuService = menuService;
        this.telegramSender = telegramSender;
        this.userService = userService;
        this.orderTgService = orderTgService;
    }

    @Override
    public void execute(Order command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = groupUser.first();
        final var giver = groupUser.second();
        if (command.itemId().isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(group.id())
                .text(TavernMenuLocalization.itemNotInMenu(group.language()))
                .replyMessageId(command.messageId())
                .build()
            );
            return;
        }
        final var menuItemResult = menuService.getAvailableMenuItem(command.itemId().get());
        if (menuItemResult.isEmpty()) {
            telegramSender.send(SendMessageBuilder.builder()
                .chatId(group.id())
                .text(TavernMenuLocalization.itemNotInMenu(group.language()))
                .replyMessageId(command.messageId())
                .build()
            );
            return;
        }
        final var menuItem = menuItemResult.get();
        final User acceptor;
        if (command.replyInfo().isEmpty()) {
            acceptor = giver;
        } else {
            final var replyInfo = command.replyInfo().get();
            if (processReplyOwner(replyInfo.messageOwner(), group).isLeft()) {
                return;
            }
            acceptor = userService.getOrCreateFromGroup(replyInfo.userId());
        }
        orderTgService.orderMenuItem(group, giver, acceptor, menuItem)
            .peekLeft(
                error -> telegramSender.send(
                    SendMessageBuilder
                        .builder()
                        .chatId(group.id())
                        .text(error.text(group.language()))
                        .replyMessageId(command.messageId())
                        .build()
                )
            );
    }

    private Either<Failure, Success> processReplyOwner(MessageOwner messageOwner, Group group) {
        return switch (messageOwner) {
            case USER -> Either.right(Success.INSTANCE);
            case DIFFERENT_BOT -> {
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(group.id())
                    .text(TavernMenuLocalization.orderGiftToDifferentBot(group.language()))
                    .build()
                );
                yield Either.left(new Failure());
            }
            case THIS_BOT -> {
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(group.id())
                    .text(TavernMenuLocalization.orderGiftToThisBot(group.language()))
                    .build()
                );
                yield Either.left(new Failure());
            }
        };
    }
}
