package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.homyakin.seeker.game.duel.models.ProcessDuelError;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.utils.models.Success;

public abstract class ProcessDuelExecutor<T extends ProcessDuel> extends CommandExecutor<T> {
    private static final Logger logger = LoggerFactory.getLogger(ProcessDuelExecutor.class);
    protected final TelegramSender telegramSender;
    private final GroupUserService groupUserService;

    protected ProcessDuelExecutor(TelegramSender telegramSender, GroupUserService groupUserService) {
        this.telegramSender = telegramSender;
        this.groupUserService = groupUserService;
    }

    @Override
    public final void execute(T command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var user = groupUser.second();
        final var group = groupUser.first();

        processDuel(command, group, user)
            .peekLeft(error -> {
                    switch (error) {
                        case ProcessDuelError.DuelIsFinished duelIsFinished -> {
                            logger.warn("Duel {} already finished", command.duelId());
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
                        // TODO надо поэксперементировать с cache_time, непонятно действует он в разрезе юзера или нет
                        case ProcessDuelError.NotDuelAcceptor notDuelAcceptor -> telegramSender.send(
                            TelegramMethods.createAnswerCallbackQuery(
                                command.callbackId(),
                                DuelLocalization.notDuelAcceptingPersonage(group.language())
                            )
                        );
                    }
                }
            );
    }

    protected abstract Either<ProcessDuelError, Success> processDuel(T command, GroupTg group, User acceptor);
}
