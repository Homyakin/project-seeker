package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.taver_menu.OrderTgService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ThrowOrderExecutor extends CommandExecutor<ThrowOrder> {
    private final GroupUserService groupUserService;
    private final TelegramSender telegramSender;
    private final OrderTgService orderTgService;

    public ThrowOrderExecutor(
        GroupUserService groupUserService,
        TelegramSender telegramSender,
        OrderTgService orderTgService
    ) {
        this.groupUserService = groupUserService;
        this.telegramSender = telegramSender;
        this.orderTgService = orderTgService;
    }

    @Override
    public void execute(ThrowOrder command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = groupUser.first();
        final var throwing = groupUser.second();

        final var text = orderTgService.throwOrder(throwing, command.mentionInfo(), group.id())
                .fold(
                    error -> TavernMenuLocalization.throwOrderError(group.language(), error),
                    result -> TavernMenuLocalization.throwResult(group.language(), result)
                );

        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(text)
                .build()
        );
    }
}
