package ru.homyakin.seeker.telegram.command.user.profile;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.event.CancelEventCommand;
import ru.homyakin.seeker.game.personage.event.CancelError;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class CancelEventExecutor extends CommandExecutor<CancelEvent> {
    private final UserService userService;
    private final CancelEventCommand cancelEventCommand;
    private final TelegramSender telegramSender;

    public CancelEventExecutor(
        UserService userService,
        CancelEventCommand cancelEventCommand,
        TelegramSender telegramSender
    ) {
        this.userService = userService;
        this.cancelEventCommand = cancelEventCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(CancelEvent command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var personageId = user.personageId();
        final var language = user.language();
        final var text = cancelEventCommand.execute(personageId, command.launchedEventId())
            .fold(
                error -> switch (error) {
                    case CancelError.AlreadyFinished _,
                         CancelError.NotFound _ -> CommonLocalization.cancelEventNotFound(language);
                    case CancelError.Locked _ -> CommonLocalization.cancelEventLocked(language);
                },
                spentEnergy -> CommonLocalization.cancelEventSuccess(language, spentEnergy)
            );
        telegramSender.send(SendMessageBuilder.builder()
            .chatId(command.userId().value())
            .text(text)
            .build()
        );
    }
}
