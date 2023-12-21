package ru.homyakin.seeker.telegram.command.group.duel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.ProcessDuelError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;

@Component
public class DeclineDuelExecutor extends CommandExecutor<DeclineDuel> {
    private static final Logger logger = LoggerFactory.getLogger(DeclineDuelExecutor.class);
    private final GroupUserService groupUserService;
    private final DuelService duelService;
    private final TelegramSender telegramSender;
    private final PersonageService personageService;
    private final UserService userService;

    public DeclineDuelExecutor(
        GroupUserService groupUserService,
        DuelService duelService,
        TelegramSender telegramSender,
        PersonageService personageService,
        UserService userService
    ) {
        this.groupUserService = groupUserService;
        this.duelService = duelService;
        this.telegramSender = telegramSender;
        this.personageService = personageService;
        this.userService = userService;
    }

    @Override
    public void execute(DeclineDuel command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var duel = duelService.getByIdForce(command.duelId());
        final var user = groupUser.second();
        final var group = groupUser.first();

        // TODO в сервис
        if (!duel.acceptingPersonageId().equals(user.personageId())) {
            telegramSender.send(
                TelegramMethods.createAnswerCallbackQuery(
                    command.callbackId(),
                    DuelLocalization.notDuelAcceptingPersonage(group.language())
                )
            );
            return;
        }

        duelService.declineDuel(duel)
            .peek(success -> {
                final var initiatingPersonage = personageService.getByIdForce(duel.initiatingPersonageId());
                final var initiatingUser = userService.getByPersonageIdForce(duel.initiatingPersonageId());
                telegramSender.send(EditMessageTextBuilder.builder()
                    .chatId(group.id())
                    .messageId(command.messageId())
                    .text(DuelLocalization.declinedDuel(group.language(), TgPersonageMention.of(initiatingPersonage, initiatingUser.id())))
                    .build()
                );
            })
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
}
