package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.Localization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.utils.models.Failure;

@Component
public class DeclineDuelExecutor extends CommandExecutor<DeclineDuel> {
    private static final Logger logger = LoggerFactory.getLogger(DeclineDuelExecutor.class);
    private final GroupUserService groupUserService;
    private final DuelService duelService;
    private final TelegramSender telegramSender;

    public DeclineDuelExecutor(
        GroupUserService groupUserService,
        DuelService duelService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.duelService = duelService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(DeclineDuel command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var duel = duelService.getByIdForce(command.duelId());
        final var user = groupUser.second();
        final var group = groupUser.first();

        if (duel.acceptingPersonageId() != user.personageId()) {
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    Localization.get(group.language()).notDuelAcceptingPersonage()
                )
            );
            return;
        }
        
        duelService.declineDuel(duel.id());
        telegramSender.send(
            TelegramMethods.createEditMessageText(
                group.id(),
                command.messageId(),
                Localization.get(group.language()).declinedDuel()
            )
        );
    }
}
