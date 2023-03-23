package ru.homyakin.seeker.telegram.command.user.navigation;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ReceptionDeskExecutor extends CommandExecutor<ReceptionDesk> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public ReceptionDeskExecutor(UserService userService, TelegramSender telegramSender) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ReceptionDesk command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(CommonLocalization.receptionDesk(user.language()))
            .keyboard(ReplyKeyboards.receptionDeskKeyboard(user.language()))
            .build()
        );
    }
}
