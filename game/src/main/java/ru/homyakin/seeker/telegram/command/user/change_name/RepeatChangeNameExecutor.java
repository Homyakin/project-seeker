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
public class RepeatChangeNameExecutor extends CommandExecutor<RepeatChangeName> {
    private final UserService userService;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;

    public RepeatChangeNameExecutor(
        UserService userService,
        UserStateService userStateService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(RepeatChangeName command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        userStateService.setUserState(user, new ChangeNameState.InitChangeName());
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(ChangeNameLocalization.repeatChangeName(user.language()))
            .keyboard(ReplyKeyboards.initChangeNameKeyboard(user.language()))
            .build()
        );
    }
}
