package ru.homyakin.seeker.telegram.command.user.contraband;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.contraband.action.ContrabandService;
import ru.homyakin.seeker.game.contraband.entity.ReceiverContrabandError;
import ru.homyakin.seeker.locale.contraband.ContrabandLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.contraband.TgContrabandNotifier;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;

@Component
public class OpenContrabandAsReceiverExecutor extends CommandExecutor<OpenContrabandAsReceiver> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ContrabandService contrabandService;
    private final TgContrabandNotifier contrabandNotifier;

    public OpenContrabandAsReceiverExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ContrabandService contrabandService,
        TgContrabandNotifier contrabandNotifier
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.contrabandService = contrabandService;
        this.contrabandNotifier = contrabandNotifier;
    }

    @Override
    public void execute(OpenContrabandAsReceiver command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var language = user.language();

        final var result = contrabandService.openAsReceiver(command.contrabandId(), user.personageId());
        result.peek(openResult -> {
            final var contraband = contrabandService.getById(command.contrabandId()).orElseThrow();
            contrabandNotifier.sendEchoToFinder(contraband, openResult);
            final var text = ContrabandLocalization.openResult(language, openResult);
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(text)
                .build()
            );
        }).peekLeft(error -> {
            final var text = switch (error) {
                case ReceiverContrabandError.Expired _ -> ContrabandLocalization.contrabandExpiredError(language);
                case ReceiverContrabandError.NotFound _, ReceiverContrabandError.NotReceiver _ ->
                    ContrabandLocalization.contrabandAlreadyProcessed(language);
            };
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(text)
                .build()
            );
        });
    }
}
