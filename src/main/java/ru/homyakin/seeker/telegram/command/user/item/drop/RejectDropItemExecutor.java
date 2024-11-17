package ru.homyakin.seeker.telegram.command.user.item.drop;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;

@Component
public class RejectDropItemExecutor extends CommandExecutor<RejectDropItem> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public RejectDropItemExecutor(
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(RejectDropItem command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        telegramSender.send(
            EditMessageTextBuilder.builder()
                .text(ItemLocalization.rejectedDrop(user.language()))
                .chatId(user.id())
                .messageId(command.messageId())
                .build()
        );
    }
}
