package ru.homyakin.seeker.telegram.command.user.change_name;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.personal.ChangeNameLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.state.ChangeNameState;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class InitChangeNameExecutor extends CommandExecutor<InitChangeName> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;

    public InitChangeNameExecutor(
        UserService userService,
        UserStateService userStateService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(InitChangeName command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        userStateService.setUserState(user, new ChangeNameState.InitChangeName());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ChangeNameLocalization.initChangeName(user.language()))
            .keyboard(ReplyKeyboards.initChangeNameKeyboard(user.language()))
            .build()
        );
    }
}
