package ru.homyakin.seeker.telegram.command.user.navigation;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.telegram.user.UserService;

@Component
public class StartUserExecutor extends CommandExecutor<StartUser> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;

    public StartUserExecutor(UserService userService, UserStateService userStateService, TelegramSender telegramSender) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(StartUser command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        userStateService.clearUserState(user);
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(CommonLocalization.welcomeUser(user.language()))
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }
}
