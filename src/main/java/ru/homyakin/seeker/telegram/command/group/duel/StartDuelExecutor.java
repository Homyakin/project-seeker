package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.CreateDuelError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.TelegramMethods;
import ru.homyakin.seeker.utils.models.Failure;

@Component
public class StartDuelExecutor extends CommandExecutor<StartDuel> {
    private static final Logger logger = LoggerFactory.getLogger(StartDuelExecutor.class);
    private final GroupUserService groupUserService;
    private final UserService userService;
    private final PersonageService personageService;
    private final DuelService duelService;
    private final TelegramSender telegramSender;

    public StartDuelExecutor(
        GroupUserService groupUserService,
        UserService userService,
        PersonageService personageService,
        DuelService duelService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.userService = userService;
        this.personageService = personageService;
        this.duelService = duelService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(StartDuel command) {
        final var groupUserPair = groupUserService.getAndActivateOrCreate(
            command.groupId(), command.userId()
        );
        final var group = groupUserPair.first();
        final var user = groupUserPair.second();
        final var result = validateCommandAndSendErrorIfNeed(command, group);
        if (result.isLeft()) {
            return;
        }
        final var replyInfo = result.get();
        final var acceptingUser = userService.getOrCreateFromGroup(replyInfo.userId());

        final var initiatingPersonage = personageService.getByIdForce(user.personageId());
        final var acceptingPersonage = personageService.getByIdForce(acceptingUser.personageId());

        final var duelResult = duelService.createDuel(initiatingPersonage, acceptingPersonage, group.id());
        if (duelResult.isLeft()) {
            final var error = duelResult.getLeft();
            // TODO поменять на красивый switch, когда выйдет из превью
            final String message;
            if (error instanceof CreateDuelError.PersonageAlreadyHasDuel) {
                message = DuelLocalization.personageAlreadyStartDuel(group.language());
            } else if (error instanceof CreateDuelError.InitiatingPersonageHasLowHealth) {
                message = DuelLocalization.duelWithInitiatorLowHealth(group.language());
            } else if (error instanceof CreateDuelError.AcceptingPersonageHasLowHealth) {
                message = DuelLocalization.duelWithAcceptorLowHealth(group.language());
            } else {
                throw new IllegalStateException("Unknown duel error: " + error.toString());
            }
            telegramSender.send(
                TelegramMethods.createSendMessage(group.id(), message)
            );
            return;
        }
        final var telegramResult = telegramSender.send(
            TelegramMethods.createSendMessage(
                group.id(),
                DuelLocalization.initDuel(group.language(), initiatingPersonage, acceptingPersonage),
                replyInfo.messageId(),
                InlineKeyboards.duelKeyboard(group.language(), duelResult.get().id())
            )
        );
        if (telegramResult.isLeft()) {
            logger.error("Can't send duel to group");
            return;
        }
        duelService.addMessageIdToDuel(duelResult.get().id(), telegramResult.get().getMessageId());
    }

    private Either<Failure, StartDuel.ReplyInfo> validateCommandAndSendErrorIfNeed(StartDuel command, Group group) {
        if (command.replyInfo().isEmpty()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(command.groupId(), DuelLocalization.duelMustBeReply(group.language()))
            );
            return Either.left(new Failure());
        }
        final var replyInfo = command.replyInfo().get();
        if (replyInfo.userId() == command.userId()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(command.groupId(), DuelLocalization.duelWithYourself(group.language()))
            );
            return Either.left(new Failure());
        }
        if (replyInfo.isBot()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(command.groupId(), DuelLocalization.duelReplyMustBeToUser(group.language()))
            );
            return Either.left(new Failure());
        }
        return Either.right(replyInfo);
    }

}
