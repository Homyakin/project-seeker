package ru.homyakin.seeker.telegram.command.group.duel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.DuelResult;
import ru.homyakin.seeker.game.duel.models.ProcessDuelError;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.group.stats.GroupStatsService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class AcceptDuelExecutor extends CommandExecutor<AcceptDuel> {
    private static final Logger logger = LoggerFactory.getLogger(AcceptDuelExecutor.class);
    private final GroupUserService groupUserService;
    private final DuelService duelService;
    private final TelegramSender telegramSender;
    private final GroupStatsService groupStatsService;
    private final UserService userService;

    public AcceptDuelExecutor(
        GroupUserService groupUserService,
        DuelService duelService,
        TelegramSender telegramSender,
        GroupStatsService groupStatsService,
        UserService userService
    ) {
        this.groupUserService = groupUserService;
        this.duelService = duelService;
        this.telegramSender = telegramSender;
        this.groupStatsService = groupStatsService;
        this.userService = userService;
    }

    @Override
    public void execute(AcceptDuel command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var duel = duelService.getByIdForce(command.duelId());
        final var acceptingUser = groupUser.second();
        final var group = groupUser.first();

        // TODO в сервис
        if (!duel.acceptingPersonageId().equals(acceptingUser.personageId())) {
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    DuelLocalization.notDuelAcceptingPersonage(group.language())
                )
            );
            return;
        }

        duelService.finishDuel(duel)
            .peek(result -> processDuelResult(result, acceptingUser, command, group))
            .peekLeft(error -> {
                    switch (error) {
                        case ProcessDuelError.DuelIsFinished duelIsFinished -> {
                            logger.warn("Duel {} already finished", duel.id());
                            telegramSender.send(EditMessageTextBuilder.builder()
                                .chatId(group.id())
                                .messageId(command.messageId())
                                .text(command.currentText())
                                .build()
                            );
                            telegramSender.send(
                                TelegramMethods.createAnswerCallbackQuery(
                                    command.callbackId(),
                                    DuelLocalization.duelAlreadyFinished(group.language())
                                )
                            );
                        }
                        case ProcessDuelError.DuelLocked duelLocked -> telegramSender.send(
                            TelegramMethods.createAnswerCallbackQuery(
                                command.callbackId(),
                                DuelLocalization.duelIsLocked(group.language())
                            )
                        );
                    }
                }
            );

    }

    private void processDuelResult(DuelResult result, User acceptingUser, AcceptDuel command, Group group) {
        final User winnerUser;
        final User loserUser;
        if (result.winner().personage().id().equals(acceptingUser.personageId())) {
            winnerUser = acceptingUser;
            loserUser = userService.getByPersonageIdForce(result.loser().personage().id());
        } else {
            winnerUser = userService.getByPersonageIdForce(result.winner().personage().id());
            loserUser = acceptingUser;
        }
        groupStatsService.increaseDuelsComplete(command.groupId(), winnerUser.personageId(), loserUser.personageId());

        telegramSender.send(
            EditMessageTextBuilder.builder()
                .chatId(group.id())
                .messageId(command.messageId())
                .text(finishedDuelText(group.language(), result, winnerUser, loserUser))
                .build()
        );
    }

    // TODO в локализацию
    private String finishedDuelText(
        Language language,
        DuelResult duelResult,
        User winnerUser,
        User loserUser
    ) {
        return DuelLocalization.finishedDuel(
            language,
            TgPersonageMention.of(duelResult.winner().personage(), winnerUser.id()),
            TgPersonageMention.of(duelResult.loser().personage(), loserUser.id())
        ) + "\n\n" +
            DuelLocalization.personageDuelResult(language, duelResult.winner()) + "\n" +
            DuelLocalization.personageDuelResult(language, duelResult.loser());
    }
}
