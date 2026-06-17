package ru.homyakin.seeker.telegram.command.user.change_name;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
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
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public InitChangeNameExecutor(
        UserService userService,
        UserStateService userStateService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.userStateService = userStateService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(InitChangeName command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var builder = SendMessageBuilder.builder().chatId(user.id());
        final var text = personageService.initChangeName(user.personageId())
            .fold(
                error -> ChangeNameLocalization.notEnoughMoney(user.language(), error.neededMoney()),
                money -> {
                    userStateService.setUserState(user, new ChangeNameState.InitChangeName());
                    builder.keyboard(ReplyKeyboards.initChangeNameKeyboard(user.language()));
                    return ChangeNameLocalization.initChangeName(user.language(), money);
                }
            );
        telegramSender.send(builder.text(text).build());
    }
}
