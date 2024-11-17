package ru.homyakin.seeker.telegram.command.user.navigation;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.user.action.AddUsertgReferer;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.ReplyKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class StartUserExecutor extends CommandExecutor<StartUser> {
    private final UserService userService;
    private final AddUsertgReferer addUsertgReferer;
    private final UserStateService userStateService;
    private final TelegramSender telegramSender;

    public StartUserExecutor(
        UserService userService,
        AddUsertgReferer addUsertgReferer,
        UserStateService userStateService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.addUsertgReferer = addUsertgReferer;
        this.userStateService = userStateService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(StartUser command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        userStateService.clearUserState(user);
        if (command.params().containsKey(TextConstants.TG_START_REFERER_PARAM)) {
            addUsertgReferer.addReferer(
                user.id(),
                command.params().get(TextConstants.TG_START_REFERER_PARAM),
                command.time().atZone(TimeUtils.moscowZone())
            );
        }
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(CommonLocalization.welcomeUser(user.language()))
            .keyboard(ReplyKeyboards.mainKeyboard(user.language()))
            .build()
        );
    }
}
