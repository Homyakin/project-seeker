package ru.homyakin.seeker.telegram.command.user.setting;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.settings.action.SetSettingValueCommand;
import ru.homyakin.seeker.locale.personal.SettingsLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;

@Component
public class SetPersonageSettingExecutor extends CommandExecutor<SetPersonageSetting> {
    private final UserService userService;
    private final SetSettingValueCommand setSettingValueCommand;
    private final TelegramSender telegramSender;

    public SetPersonageSettingExecutor(
        UserService userService,
        TelegramSender telegramSender,
        SetSettingValueCommand setSettingValueCommand
    ) {
        this.userService = userService;
        this.setSettingValueCommand = setSettingValueCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(SetPersonageSetting command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var settings = setSettingValueCommand.execute(user.personageId(), command.setting(), command.value());

        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(SettingsLocalization.settings(user.language()))
                .keyboard(InlineKeyboards.personageSettingsKeyboard(user.language(), settings))
                .build()
        );
    }

}
