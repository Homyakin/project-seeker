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

        final var initiatorPersonage = personageService.getByIdForce(user.personageId());
        final var acceptingPersonage = personageService.getByIdForce(acceptingUser.personageId());

        final var duelResult = duelService.createDuel(initiatorPersonage.id(), acceptingPersonage.id(), group.id());
        if (duelResult.isLeft()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(group.id(), Localization.get(group.language()).personageAlreadyStartDuel())
            );
            return;
        }
        final var telegramResult = telegramSender.send(
            TelegramMethods.createSendMessage(
                group.id(),
                Localization.get(group.language()).initDuel().formatted(
                    initiatorPersonage.level(),
                    initiatorPersonage.name(),
                    acceptingPersonage.level(),
                    acceptingPersonage.name()
                ),
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
                TelegramMethods.createSendMessage(command.groupId(), Localization.get(group.language()).duelMustBeReply())
            );
            return Either.left(new Failure());
        }
        final var replyInfo = command.replyInfo().get();
        if (replyInfo.userId() == command.userId()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(command.groupId(), Localization.get(group.language()).duelWithYourself())
            );
            return Either.left(new Failure());
        }
        if (replyInfo.isBot()) {
            telegramSender.send(
                TelegramMethods.createSendMessage(command.groupId(), Localization.get(group.language()).duelReplyMustBeToUser())
            );
            return Either.left(new Failure());
        }
        return Either.right(replyInfo);
    }

}
