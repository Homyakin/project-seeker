package ru.homyakin.seeker.telegram.command.group.duel;

import io.vavr.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.DuelService;
import ru.homyakin.seeker.game.duel.models.DuelError;
import ru.homyakin.seeker.game.personage.PersonageService;
import ru.homyakin.seeker.locale.duel.DuelLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.Group;
import ru.homyakin.seeker.telegram.models.ReplyInfo;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;
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
        final var initiatingUser = groupUserPair.second();
        final var result = validateCommandAndSendErrorIfNeed(command, group);
        if (result.isLeft()) {
            return;
        }
        final var replyInfo = result.get();
        final var acceptingUser = userService.getOrCreateFromGroup(replyInfo.userId());

        final var initiatingPersonage = personageService.getByIdForce(initiatingUser.personageId());
        final var acceptingPersonage = personageService.getByIdForce(acceptingUser.personageId());

        final var duelResult = duelService.createDuel(initiatingPersonage, acceptingPersonage, group.id());
        if (duelResult.isLeft()) {
            final var error = duelResult.getLeft();
            // TODO поменять на красивый switch, когда выйдет из превью
            final String message;
            if (error instanceof DuelError.PersonageAlreadyHasDuel) {
                message = DuelLocalization.personageAlreadyStartDuel(group.language());
            } else if (error instanceof DuelError.InitiatingPersonageNotEnoughMoney notEnoughMoney) {
                message = DuelLocalization.duelWithInitiatorNotEnoughMoney(group.language(), notEnoughMoney.money());
            } else {
                throw new IllegalStateException("Unknown duel error: " + error.toString());
            }
            telegramSender.send(
                SendMessageBuilder.builder().chatId(group.id()).text(message).build()
            );
            return;
        }
        final var telegramResult = telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(
                    DuelLocalization.initDuel(
                        group.language(),
                        TgPersonageMention.of(initiatingPersonage, initiatingUser.id()),
                        TgPersonageMention.of(acceptingPersonage, acceptingUser.id())
                    )
                )
                .replyMessageId(replyInfo.messageId())
                .keyboard(InlineKeyboards.duelKeyboard(group.language(), duelResult.get().id()))
                .build()
        );
        if (telegramResult.isLeft()) {
            logger.error("Can't send duel to group");
            return;
        }
        duelService.addMessageIdToDuel(duelResult.get().id(), telegramResult.get().getMessageId());
    }

    private Either<Failure, ReplyInfo> validateCommandAndSendErrorIfNeed(StartDuel command, Group group) {
        if (command.replyInfo().isEmpty()) {
            telegramSender.send(
                SendMessageBuilder.builder().chatId(command.groupId()).text(DuelLocalization.duelMustBeReply(group.language())).build()
            );
            return Either.left(new Failure());
        }
        final var replyInfo = command.replyInfo().get();
        if (replyInfo.userId() == command.userId()) {
            telegramSender.send(
                SendMessageBuilder.builder().chatId(command.groupId()).text(DuelLocalization.duelWithYourself(group.language())).build()
            );
            return Either.left(new Failure());
        }
        return switch (replyInfo.messageOwner()) {
            case USER -> Either.right(replyInfo);
            case DIFFERENT_BOT -> {
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(command.groupId())
                    .text(DuelLocalization.duelWithDifferentBot(group.language()))
                    .build()
                );
                yield Either.left(new Failure());
            }
            case THIS_BOT -> {
                telegramSender.send(SendMessageBuilder.builder()
                    .chatId(command.groupId())
                    .text(DuelLocalization.duelWithThisBot(group.language()))
                    .build()
                );
                yield Either.left(new Failure());
            }
        };
    }

}
