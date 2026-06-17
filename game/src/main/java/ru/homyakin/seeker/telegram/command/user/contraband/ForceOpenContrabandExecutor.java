package ru.homyakin.seeker.telegram.command.user.contraband;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.contraband.action.ContrabandService;
import ru.homyakin.seeker.game.contraband.entity.FinderContrabandError;
import ru.homyakin.seeker.locale.contraband.ContrabandLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;

@Component
public class ForceOpenContrabandExecutor extends CommandExecutor<ForceOpenContraband> {
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ContrabandService contrabandService;

    public ForceOpenContrabandExecutor(
        UserService userService,
        TelegramSender telegramSender,
        ContrabandService contrabandService
    ) {
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.contrabandService = contrabandService;
    }

    @Override
    public void execute(ForceOpenContraband command) {
        final var user = userService.forceGetFromPrivate(command.userId());
        final var language = user.language();

        final var result = contrabandService.forceOpen(command.contrabandId(), user.personageId());
        result.peek(openResult -> {
            final var text = ContrabandLocalization.openResult(language, openResult);
            telegramSender.send(EditMessageTextBuilder.builder()
                .chatId(user.id())
                .messageId(command.messageId())
                .text(text)
                .build()
            );
        }).peekLeft(error -> {
            final var text = switch (error) {
                case FinderContrabandError.Expired _ -> ContrabandLocalization.contrabandExpiredError(language);
                case FinderContrabandError.NotFound _, FinderContrabandError.NotOwner _,
                     FinderContrabandError.AlreadyProcessed _ ->
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
