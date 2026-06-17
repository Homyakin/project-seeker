package ru.homyakin.seeker.telegram.command.user.setting;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.personal.SettingsLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class HidePersonageExecutor extends CommandExecutor<ToggleHidePersonage> {
    private final UserService userService;
    private final PersonageService personageService;
    private final TelegramSender telegramSender;

    public HidePersonageExecutor(
        UserService userService,
        PersonageService personageService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.personageService = personageService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(ToggleHidePersonage command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var result = personageService.toggleIsHidden(user.personageId());
        final String text;
        // TODO переделать на switch когда там будут примитивы
        if (result) {
            text = SettingsLocalization.personageIsHidden(user.language());
        } else {
            text = SettingsLocalization.personageIsUnhidden(user.language());
        }
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(user.id())
            .text(text)
            .build()
        );
    }

}
