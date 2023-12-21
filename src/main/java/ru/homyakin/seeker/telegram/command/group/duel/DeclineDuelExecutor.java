package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.ProcessDuelError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.utils.models.Success;

@Component
public class DeclineDuelExecutor extends ProcessDuelExecutor<DeclineDuel> {
    private final DuelService duelService;
    private final PersonageService personageService;
    private final UserService userService;

    public DeclineDuelExecutor(
        GroupUserService groupUserService,
        DuelService duelService,
        TelegramSender telegramSender,
        PersonageService personageService,
        UserService userService
    ) {
        super(telegramSender, groupUserService);
        this.duelService = duelService;
        this.personageService = personageService;
        this.userService = userService;
    }

    @Override
    protected Either<ProcessDuelError, Success> processDuel(DeclineDuel command, Group group, User acceptor) {
        final var duel = duelService.getByIdForce(command.duelId());
        return duelService.declineDuel(duel, acceptor.personageId())
            .peek(success -> {
                final var initiatingPersonage = personageService.getByIdForce(duel.initiatingPersonageId());
                final var initiatingUser = userService.getByPersonageIdForce(duel.initiatingPersonageId());
                telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(group.id())
                    .messageId(command.messageId())
                    .text(DuelLocalization.declinedDuel(group.language(), TgPersonageMention.of(initiatingPersonage, initiatingUser.id())))
                    .build()
                );
            });
    }
}
