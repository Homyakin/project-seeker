package ru.homyakin.seeker.telegram.command.user.characteristics;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;

@Component
public class CancelResetCharacteristicsExecutor extends CommandExecutor<CancelResetCharacteristics> {
    private final UserService userService;
    private final TelegramSender telegramSender;

    public CancelResetCharacteristicsExecutor(
        UserService userService,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(CancelResetCharacteristics command) {
        final var user = userService.getOrCreateFromPrivate(command.userId());
        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(command.userId())
            .messageId(command.messageId())
            .text(CharacteristicLocalization.canceledReset(user.language()))
            .build()
        );
    }
}
