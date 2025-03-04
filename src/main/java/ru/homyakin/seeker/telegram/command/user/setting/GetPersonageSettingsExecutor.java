package ru.homyakin.seeker.telegram.command.user.setting;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.settings.action.GetPersonageSettingsCommand;
import ru.homyakin.seeker.locale.personal.SettingsLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GetPersonageSettingsExecutor extends CommandExecutor<GetPersonageSettings> {
    private final UserService userService;
    private final GetPersonageSettingsCommand getPersonageSettingsCommand;
    private final TelegramSender telegramSender;

    public GetPersonageSettingsExecutor(
        UserService userService,
        TelegramSender telegramSender,
        GetPersonageSettingsCommand getPersonageSettingsCommand
    ) {
        this.userService = userService;
        this.getPersonageSettingsCommand = getPersonageSettingsCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GetPersonageSettings command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var settings = getPersonageSettingsCommand.execute(user.personageId());

        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(SettingsLocalization.settings(user.language()))
            .keyboard(InlineKeyboards.personageSettingsKeyboard(user.language(), settings))
            .build()
        );
    }

}
